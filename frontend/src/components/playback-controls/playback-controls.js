import React, {useContext, useEffect, useState} from 'react';
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";
import {formatTimeFromSeconds} from "../../formatters";
import {ShuffleChaos} from "./shuffle-chaos/shuffle-chaos";
import {SocketContext} from "../../services/socket-provider";
import {getVolumeIcon, isSafari} from "../../util";
import {PlaybackContext} from "../../services/playback-provider";
import {markTrackListened} from "../../services/mark-track-listened";
import {setMediaKeyTrack, setupMediaKeySessionIfNeeded} from "../../media-key";

const originalTitle = document.title;

// State we don't want to render on
let loadingTrackId = null;
let lastSongPlayHeartbeatTime = 0;
let lastTime = 0;
let lastTimePlayedOverride = -1;
let timeTarget = null;
let totalTimeListened = 0;
let listenedTo = false;

// In a functional component we need to keep around some previous state to do things when changes occur
let initialStateSent = false;
let previousPlaying = false;
let previousCurrentSessionPlayCounter = 0;

export default function PlaybackControls() {
	const [currentSessionPlayCounter, setCurrentSessionPlayCounter] = useState(0);
	const [currentTimePercent, setCurrentTimePercent] = useState(0);
	const [duration, setDuration] = useState(0);
	const [songUrl, setSongUrl] = useState(null);

	const musicContext = useContext(MusicContext);
	const playbackContext = useContext(PlaybackContext);
	const socketContext = useContext(SocketContext);

	setupMediaKeySessionIfNeeded(playbackContext, musicContext);

	const handleSongEnd = () => {
		const playingNewSong = musicContext.playNext();
		if (!playingNewSong) {
			setPageTitle(null);
			playbackContext.setProviderState({ isPlaying: false });
		}
	};

	// You might think that this could be calculated in handleSongChange() and not need its own function. However,
	// the duration is NOT YET KNOWN when the song changes, because it hasn't fully loaded the metadata. This event
	// triggers some time after the song change, once the metadata itself is loaded
	const handleDurationChange = event => {
		const duration = event.target.duration;
		// If someone listens to 60% of a song, we want to mark it as listened to. Keep track of what that target is
		timeTarget = duration * 0.60;
		setDuration(duration);
	};

	const handleSongChange = () => {
		if (musicContext.playedTrackIndex === null) {
			playbackContext.setProviderState({ isPlaying: false });
			setPageTitle(null);
			return;
		}

		const newTrack = musicContext.playedTrack;

		if (newTrack.id === loadingTrackId) {
			return;
		}

		loadingTrackId = newTrack.id;

		// Safari does not support the superior OGG format. Use MP3 instead for them
		const audioFormat = isSafari() ? 'MP3' : 'OGG';
		Api.get('file/link/' + newTrack.id, { audioFormat }).then(links => {
			if (musicContext.playedTrack && musicContext.playedTrack.id !== loadingTrackId) {
				// The song we chose is no longer the active one. Don't acknowledge it.
				// This can easily happen if using "play next" multiple times quickly
				return;
			}

			musicContext.setProviderState({ playedAlbumArtUrl: links.albumArtLink }, musicContext.forceTrackUpdate);

			lastTime = 0;
			listenedTo = false;
			totalTimeListened = 0;
			setDuration(0);
			playbackContext.setProviderState({ isPlaying: true });
			setPageTitle(newTrack);
			setSongUrl(links.songLink);

			setMediaKeyTrack(newTrack, links.albumArtLink);

			previousCurrentSessionPlayCounter = currentSessionPlayCounter;
			setCurrentSessionPlayCounter(musicContext.sessionPlayCounter);

			loadingTrackId = null;

			const audio = document.getElementById('audio');
			audio.currentTime = 0;
			audio.src = links.songLink;
			const playPromise = audio.play();
			playPromise.catch(e => {
				// Code 20 is when the loading gets aborted. This happens all the time if you skip around.
				// I'm sick of seeing them in the logs so ignore them
				if (e.code !== 20) {
					console.error(e);
				}
			})
		});
	};

	// Send an event that says we are listening to a particular song so other people can see it.
	// Pass these values in as params so they aren't captured from the useEffect() and unchanging
	const broadcastListenHeartbeatIfNeeded = (currentTimePercent, forceBroadcast) => {
		const currentTimeMillis = Date.now();
		const heartbeatInterval = 20000; // Don't need to spam everyone. Only check every 20 seconds

		if (forceBroadcast || lastSongPlayHeartbeatTime < currentTimeMillis - heartbeatInterval) {
			lastSongPlayHeartbeatTime = currentTimeMillis;

			socketContext.sendPlayEvent({
				timePlayed: currentTimePercent * duration
			});
		}
	};

	const changeVolume = event => {
		const audio = document.getElementById('audio');
		const volume = event.target.value;

		audio.volume = volume;
		playbackContext.setVolume(volume);

		socketContext.sendPlayEvent({ volume });
	};

	const changePlayTime = event => {
		const audio = document.getElementById('audio');
		const playPercent = event.target.value;

		// Don't need to update state, as an event will fire and we will handle it afterwards
		audio.currentTime = playPercent * audio.duration;

		socketContext.sendPlayEvent({ timePlayed: playPercent * duration });
	};

	const setPageTitle = track => {
		if (!track) {
			document.title = originalTitle;
		} else {
			document.title = getDisplayedSongName();
		}
	};

	const getDisplayedSongName = () => {
		const playedTrack = musicContext.playedTrack;
		if (!playedTrack) {
			return '';
		} else if (playedTrack.name && playedTrack.artist) {
			return `${playedTrack.name} - ${playedTrack.artist}`
		} else if (playedTrack.name) {
			return playedTrack.name
		} else if (playedTrack.artist) {
			return playedTrack.artist
		} else {
			return '-----'
		}
	};

	const setIsPlaying = newIsPlaying => {
		const audio = document.getElementById('audio');
		if (newIsPlaying) {
			// People seem to want clicking play without an active song to start playing the library
			// A (maybe) good improvement would be to have it respect your selected songs. But hard to do right now
			if (musicContext.playedTrack === null) {
				musicContext.playFromTrackIndex(null, true);
			} else {
				audio.play();
			}
		} else {
			audio.pause();
		}

		playbackContext.setProviderState({ isPlaying: newIsPlaying });
	};

	const toggleMute = () => {
		const audio = document.getElementById('audio');
		const newMute = !playbackContext.isMuted;
		audio.muted = newMute;

		playbackContext.setMuted(newMute);

		socketContext.sendPlayEvent({ muted: newMute });
	};

	useEffect(() => {
		const audio = document.getElementById('audio');
		audio.addEventListener('timeupdate', handleTimeTick);
		audio.addEventListener('durationchange', handleDurationChange);
		audio.addEventListener('ended', handleSongEnd);

		audio.volume = playbackContext.volume;
		audio.muted = playbackContext.isMuted;

		// Can happen if someone logs out and back in that we need to reset the state
		// that we don't render on. This was probably a terrible optimization strategy
		if (currentSessionPlayCounter === 0) {
			previousCurrentSessionPlayCounter = 0;
			previousPlaying = 0;
			initialStateSent = false;
		}

		if (!initialStateSent) {
			socketContext.addOnConnectedHandler(() => {
				socketContext.sendPlayEvent({
					removeTrack: true,
					volume: audio.volume,
					muted: audio.muted,
					timePlayed: 0,
					isShuffling: musicContext.shuffleSongs,
					isRepeating: musicContext.repeatSongs
				});
			});
			initialStateSent = true;
		}

		return () => {
			// The functional component does some weird stuff with logout and using a stale version of the
			// MusicContext upon re-log in. Do this to prevent auto-play of a "null" track upon re-log
			if (musicContext.playedTrack === null) {
				audio.src = '';
				musicContext.setProviderState({ playedAlbumArtUrl: null }, musicContext.forceTrackUpdate);
			}

			audio.removeEventListener('timeupdate', handleTimeTick);
			audio.removeEventListener('durationchange', handleDurationChange);
			audio.removeEventListener('ended', handleSongEnd);
		}
	}, [
		duration,
		playbackContext.isPlaying,
		playbackContext.timePlayedOverride,
		musicContext.playedTrack ? musicContext.playedTrack.id : 0
	]);

	const audio = document.getElementById('audio');

	if (
		audio !== null && // Can happen when logging out and back in temporarily
		((!previousPlaying && playbackContext.isPlaying) // Started playing something when we weren't playing anything
			|| (previousCurrentSessionPlayCounter !== currentSessionPlayCounter)) // Song changed
	) {
		lastSongPlayHeartbeatTime = Date.now();

		audio.play();
		socketContext.sendPlayEvent({
			track: musicContext.playedTrack,
			timePlayed: currentTimePercent * duration,
			isPlaying: playbackContext.isPlaying
		});
	} else if (previousPlaying && !playbackContext.isPlaying) {
		audio.pause();
		socketContext.sendPlayEvent({
			timePlayed: currentTimePercent * duration,
			isPlaying: playbackContext.isPlaying
		});
	}

	if (musicContext.playedTrack && musicContext.sessionPlayCounter !== currentSessionPlayCounter) {
		handleSongChange();
	}

	const handleTimeTick = event => {
		// If the timePlayed on the context doesn't match, it means we were given instructions via Remote Play
		// and we need to respond to it
		// const remotePlayAdjustment = playbackContext.timePlayed !== lastTime;
		const remotePlayAdjustment = playbackContext.timePlayedOverride !== lastTimePlayedOverride;
		const currentTime = remotePlayAdjustment ? playbackContext.timePlayedOverride : event.target.currentTime;

		const currentTimePercent = currentTime / duration;

		if (duration > 0) {
			setCurrentTimePercent(currentTimePercent);
		}

		const timeElapsed = currentTime - lastTime;
		lastTime = currentTime;

		if (remotePlayAdjustment) {
			audio.currentTime = currentTime;
			lastTimePlayedOverride = playbackContext.timePlayedOverride;
		}

		// If the time elapsed went negative, or had a large leap forward (more than 1 second), then it means that someone
		// manually altered the song's progress. Do no other checks or updates
		if (timeElapsed < 0 || timeElapsed > 1) {
			if (remotePlayAdjustment) {
				broadcastListenHeartbeatIfNeeded(currentTimePercent, true);
			}

			return;
		}

		totalTimeListened = totalTimeListened + timeElapsed;

		if (timeTarget && totalTimeListened > timeTarget && !listenedTo) {
			listenedTo = true;

			const playedTrack = musicContext.playedTrack;
			markTrackListened(playedTrack.id, () => {
				// Could grab the track data from the backend, but this update is simple to just replicate on the frontend
				playedTrack.playCount++;
				playedTrack.lastPlayed = new Date();

				// We updated the reference rather than dealing with the hassle of updating via setState for multiple collections
				// that we'd have to search and find indexes for. So issue an update to the parent component afterwards
				musicContext.forceTrackUpdate();
			});
		}

		broadcastListenHeartbeatIfNeeded(currentTimePercent);
	};

	if (audio !== null) {
		if (playbackContext.volume !== audio.volume) {
			audio.volume = playbackContext.volume;
		}
		if (audio.muted !== playbackContext.isMuted) {
			audio.muted = playbackContext.isMuted;
		}
	}

	const playedTrack = musicContext.playedTrack;
	const src = playedTrack ? songUrl : '';

	previousCurrentSessionPlayCounter = currentSessionPlayCounter;
	previousPlaying = playbackContext.isPlaying;

	return (
		<div id="playback-controls" className="d-flex song-player">
			<div>
				<audio id="audio" src={src}>
					Your browser is ancient. Be less ancient.
				</audio>

				<div className="played-song-name">
					{getDisplayedSongName()}
				</div>

				<div>
					<div className="d-flex">
						<i
							onMouseDown={() => {
								setIsPlaying(false);
								musicContext.playPrevious()
							}}
							className="fas fa-step-backward control"
						/>
						<i
							onMouseDown={() => setIsPlaying(!playbackContext.isPlaying)}
							className={`fas fa-${playbackContext.isPlaying ? 'pause' : 'play'} control`}
						/>
						<i
							onMouseDown={() => {
								setIsPlaying(false);
								musicContext.playNext()
							}}
							className="fas fa-step-forward control"
						/>
						<i
							onMouseDown={() => {
								musicContext.setShuffleSongs(!musicContext.shuffleSongs);
								socketContext.sendPlayEvent({ isShuffling: !musicContext.shuffleSongs });
							}}
							className={`fas fa-random control ${musicContext.shuffleSongs ? 'enabled' : ''}`}
						/>
						<i
							onMouseDown={() => {
								musicContext.setRepeatSongs(!musicContext.repeatSongs);
								socketContext.sendPlayEvent({ isRepeating: !musicContext.repeatSongs });
							}}
							className={`fas fa-sync-alt control ${musicContext.repeatSongs ? 'enabled' : ''}`}
						/>
					</div>

					<div className="play-time-wrapper">
						<div>
							{formatTimeFromSeconds(currentTimePercent * duration)} / {formatTimeFromSeconds(duration)}
						</div>
						<input
							type="range"
							className="time-slider"
							onChange={changePlayTime}
							min="0"
							max="1"
							step="0.01"
							value={currentTimePercent}
						/>
					</div>

					<div className="volume-wrapper">
						<i
							className={`fas ${getVolumeIcon(playbackContext.volume, playbackContext.isMuted)}`}
							onMouseDown={toggleMute}
						/>
						<input
							type="range"
							className="volume-slider"
							onChange={changeVolume}
							min="0"
							max="1"
							step="0.01"
							value={playbackContext.volume}
						/>
					</div>
				</div>
			</div>

			<div className="shuffle-wrapper">
				{ musicContext.shuffleSongs ? <ShuffleChaos/> : null }
			</div>
		</div>
	)
}
