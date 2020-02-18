import React, {useContext, useEffect, useState} from 'react';
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";
import {formatTimeFromSeconds} from "../../formatters";
import * as LocalStorage from "../../local-storage";
import {ShuffleChaos} from "./shuffle-chaos/shuffle-chaos";
import {getDeviceId} from "../../services/version";
import {SocketContext} from "../../services/socket-provider";
import {getVolumeIcon} from "../../util";

const originalTitle = document.title;

// State we don't want to render on
let loadingTrackId = null;
let lastSongPlayHeartbeatTime = 0;
let lastTime = 0;
let timeTarget = null;
let totalTimeListened = 0;
let listenedTo = false;

// In a functional component we need to keep around some previous state to do things when changes occur
let initialStateSent = false;
let previousPlaying = false;
let previousCurrentSessionPlayCounter = 0;

export default function PlaybackControls(props) {
	const [currentSessionPlayCounter, setCurrentSessionPlayCounter] = useState(0);
	const [currentTimePercent, setCurrentTimePercent] = useState(0);
	const [duration, setDuration] = useState(0);
	const [songUrl, setSongUrl] = useState(null);

	const musicContext = useContext(MusicContext);
	const socketContext = useContext(SocketContext);

	const handleSongEnd = () => {
		const playingNewSong = musicContext.playNext();
		if (!playingNewSong) {
			setPageTitle(null);
			musicContext.setProviderState({ isPlaying: false });
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
			musicContext.setProviderState({ isPlaying: false });
			setPageTitle(null);
			return;
		}

		const newTrack = musicContext.playedTrack;

		if (newTrack.id === loadingTrackId) {
			return;
		}

		loadingTrackId = newTrack.id;

		Api.get('file/link/' + newTrack.id).then(links => {
			props.setAlbumArt(links.albumArtLink);

			lastTime = 0;
			listenedTo = false;
			totalTimeListened = 0;
			setDuration(0);
			musicContext.setProviderState({ isPlaying: true });
			setPageTitle(newTrack);
			setSongUrl(links.songLink);

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
	const broadcastListenHeartbeatIfNeeded = currentTimePercent => {
		const currentTimeMillis = Date.now();
		const heartbeatInterval = 20000; // Don't need to spam everyone. Only check every 20 seconds

		if (lastSongPlayHeartbeatTime < currentTimeMillis - heartbeatInterval) {
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
		musicContext.setVolume(volume);

		socketContext.sendPlayEvent({
			timePlayed: currentTimePercent * duration,
			volume
		});
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

	const togglePause = () => {
		const audio = document.getElementById('audio');
		if (musicContext.isPlaying) {
			audio.pause();
		} else {
			// People seem to want clicking play without an active song to start playing the library
			// A (maybe) good improvement would be to have it respect your selected songs. But hard to do right now
			if (musicContext.playedTrack === null) {
				musicContext.playFromTrackIndex(null, true);
			} else {
				audio.play();
			}
		}

		musicContext.setProviderState({ isPlaying: !musicContext.isPlaying });
	};

	const toggleMute = () => {
		const audio = document.getElementById('audio');
		const newMute = !musicContext.isMuted;
		audio.muted = newMute;

		musicContext.setMuted(newMute);

		socketContext.sendPlayEvent({
			timePlayed: currentTimePercent * duration,
			muted: newMute
		});
	};

	// I hate this, but so much state is running around keeping it all in sync remotely is such a chore.
	// This is a cop out of my bad design to keep things in sync
	if (socketContext.pendingRebroadcast) {
		const audio = document.getElementById('audio');
		socketContext.sendPlayEvent({
			volume: audio.volume,
			muted: audio.muted,
			timePlayed: currentTimePercent * duration,
			isShuffling: musicContext.shuffleSongs,
			isRepeating: musicContext.repeatSongs
		}, true);
	}

	useEffect(() => {
		const audio = document.getElementById('audio');
		audio.addEventListener('timeupdate', handleTimeTick);
		audio.addEventListener('durationchange', handleDurationChange);
		audio.addEventListener('ended', handleSongEnd);

		audio.volume = musicContext.volume;
		audio.muted = musicContext.isMuted;

		if (!initialStateSent) {
			initialStateSent = true;
			socketContext.sendPlayEvent({
				removeTrack: true,
				volume: audio.volume,
				muted: audio.muted,
				timePlayed: 0,
				isShuffling: musicContext.shuffleSongs,
				isRepeating: musicContext.repeatSongs
			});
		}

		return () => {
			// The functional component does some weird stuff with logout and using a stale version of the
			// MusicContext upon re-log in. Do this to prevent auto-play of a "null" track upon re-log
			if (musicContext.playedTrack === null) {
				audio.src = '';
				props.setAlbumArt(null);
			}

			audio.removeEventListener('timeupdate', handleTimeTick);
			audio.removeEventListener('durationchange', handleDurationChange);
			audio.removeEventListener('ended', handleSongEnd);
		}
	}, [duration, musicContext.isPlaying, musicContext.playedTrack ? musicContext.playedTrack.id : 0]);

	const audio = document.getElementById('audio');
	if (
		(!previousPlaying && musicContext.isPlaying) // Started playing something when we weren't playing anything
		|| (previousCurrentSessionPlayCounter !== currentSessionPlayCounter) // Song changed
	) {
		lastSongPlayHeartbeatTime = Date.now();

		audio.play();
		socketContext.sendPlayEvent({
			track: musicContext.playedTrack,
			timePlayed: currentTimePercent * duration,
			isPlaying: musicContext.isPlaying
		});
	} else if (previousPlaying && !musicContext.isPlaying) {
		audio.pause();
		socketContext.sendPlayEvent({
			timePlayed: currentTimePercent * duration,
			isPlaying: musicContext.isPlaying
		});
	}

	if (musicContext.playedTrack && musicContext.sessionPlayCounter !== currentSessionPlayCounter) {
		handleSongChange();
	}

	const handleTimeTick = event => {
		const currentTime = event.target.currentTime;
		const currentTimePercent = currentTime / duration;

		if (duration > 0) {
			// Truncate the percentage to 2 decimal places, since our progress bar only updates in 1/100 increments.
			// Doing this allows us to skip many re-renders that do nothing.
			setCurrentTimePercent(currentTimePercent);
		}

		const timeElapsed = currentTime - lastTime;
		lastTime = currentTime;
		// If the time elapsed went negative, or had a large leap forward (more than 1 second), then it means that someone
		// manually altered the song's progress. Do no other checks or updates
		if (timeElapsed < 0 || timeElapsed > 1) {
			return;
		}

		totalTimeListened = totalTimeListened + timeElapsed;

		if (timeTarget && totalTimeListened > timeTarget && !listenedTo) {
			listenedTo = true;

			const playedTrack = musicContext.playedTrack;
			Api.post('track/mark-listened', { trackId: playedTrack.id, deviceId: getDeviceId() })
				.then(() => {
					// Could grab the track data from the backend, but this update is simple to just replicate on the frontend
					playedTrack.playCount++;
					playedTrack.lastPlayed = new Date();

					// We updated the reference rather than dealing with the hassle of updating via setState for multiple collections
					// that we'd have to search and find indexes for. So issue an update to the parent component afterwards
					musicContext.forceTrackUpdate();
				})
				.catch(e => {
					console.error('Failed to update play count');
					console.error(e);
				});
		}

		broadcastListenHeartbeatIfNeeded(currentTimePercent, musicContext.volume);
	};

	if (audio !== null && musicContext.volume !== audio.volume) {
		console.log('Adjusting volume', audio.volume, musicContext.volume);
		audio.volume = musicContext.volume;
	}

	const playedTrack = musicContext.playedTrack;
	const src = playedTrack ? songUrl : '';

	previousCurrentSessionPlayCounter = currentSessionPlayCounter;
	previousPlaying = musicContext.isPlaying;

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
							onMouseDown={musicContext.playPrevious}
							className="fas fa-step-backward control"
						/>
						<i
							onMouseDown={togglePause}
							className={`fas fa-${musicContext.isPlaying ? 'pause' : 'play'} control`}
						/>
						<i
							onMouseDown={musicContext.playNext}
							className="fas fa-step-forward control"
						/>
						<i
							onMouseDown={() => {
								musicContext.setShuffleSongs(!musicContext.shuffleSongs);
								socketContext.sendPlayEvent({
									timePlayed: currentTimePercent * duration,
									isShuffling: !musicContext.shuffleSongs
								});
							}}
							className={`fas fa-random control ${musicContext.shuffleSongs ? 'enabled' : ''}`}
						/>
						<i
							onMouseDown={() => {
								musicContext.setRepeatSongs(!musicContext.repeatSongs);
								socketContext.sendPlayEvent({
									timePlayed: currentTimePercent * duration,
									isRepeating: !musicContext.repeatSongs
								});
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
							className={`fas ${getVolumeIcon(musicContext.volume, musicContext.isMuted)}`}
							onMouseDown={toggleMute}
						/>
						<input
							type="range"
							className="volume-slider"
							onChange={changeVolume}
							min="0"
							max="1"
							step="0.01"
							value={musicContext.volume}
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
