import React, {useContext, useEffect, useState} from 'react';
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";
import {formatTimeFromSeconds} from "../../formatters";
import * as LocalStorage from "../../local-storage";
import {ShuffleChaos} from "./shuffle-chaos/shuffle-chaos";
import {getDeviceId} from "../../services/version";
import {SocketContext} from "../../services/socket-provider";

// State we don't want to render on
let loadingTrackId = null;
let lastSongPlayHeartbeatTime = 0;
let lastTime = 0;
let timeTarget = null;
let totalTimeListened = 0;
let listenedTo = false;

// In a functional component we need to keep around some previous state to do things when changes occur
let previousPlaying = false;
let previousCurrentSessionPlayCounter = 0;

export default function PlaybackControls(props) {
	const [currentSessionPlayCounter, setCurrentSessionPlayCounter] = useState(0);
	const [currentTimePercent, setCurrentTimePercent] = useState(0);
	const [duration, setDuration] = useState(0);
	const [songUrl, setSongUrl] = useState(null);
	const [volume, setVolume] = useState(1);
	const [muted, setMuted] = useState(false);
	const [playing, setPlaying] = useState(false);

	const musicContext = useContext(MusicContext);
	const socketContext = useContext(SocketContext);

	const handleSongEnd = () => {
		const playingNewSong = musicContext.playNext();
		if (!playingNewSong) {
			setPlaying(false);
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
		if (musicContext.playedTrackIndex == null) {
			setPlaying(false);
			return;
		}

		if (musicContext.playedTrack.id === loadingTrackId) {
			return;
		}

		loadingTrackId = musicContext.playedTrack.id;

		Api.get('file/link/' + musicContext.playedTrack.id).then(links => {
			props.setAlbumArt(links.albumArtLink);

			lastTime = 0;
			listenedTo = false;
			totalTimeListened = 0;
			setDuration(0);
			setPlaying(true);
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

	// Send an event that says we are listening to a particular song so other people can see it
	const broadcastListenHeartbeatIfNeeded = () => {
		const currentTimeMillis = Date.now();
		const heartbeatInterval = 15000; // Don't need to spam everyone. Only check every 15 seconds

		if (lastSongPlayHeartbeatTime < currentTimeMillis - heartbeatInterval) {
			lastSongPlayHeartbeatTime = currentTimeMillis;
			socketContext.sendPlayEvent(musicContext.playedTrack);
		}
	};

	const changeVolume = event => {
		const audio = document.getElementById('audio');
		const volume = event.target.value;

		audio.volume = volume;
		setVolume(volume);
		LocalStorage.setNumber('volume', volume);
	};

	const changePlayTime = event => {
		const audio = document.getElementById('audio');
		const playPercent = event.target.value;

		// Don't need to update state, as an event will fire and we will handle it afterwards
		audio.currentTime = playPercent * audio.duration;
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

	const getVolumeIcon = () => {
		if (muted) {
			return 'fa-volume-mute'
		} else if (volume > 0.5) {
			return 'fa-volume-up';
		} else if (volume > 0) {
			return 'fa-volume-down'
		} else {
			return 'fa-volume-off'
		}
	};

	const togglePause = () => {
		const audio = document.getElementById('audio');
		if (playing) {
			audio.pause();
		} else {
			audio.play();
		}

		setPlaying(!playing);
	};

	const toggleMute = () => {
		const audio = document.getElementById('audio');
		const newMute = !muted;
		audio.muted = newMute;

		LocalStorage.setBoolean('muted', newMute);
		setMuted(newMute);
	};

	useEffect(() => {
		const audio = document.getElementById('audio');
		audio.addEventListener('timeupdate', handleTimeTick);
		audio.addEventListener('durationchange', handleDurationChange);
		audio.addEventListener('ended', handleSongEnd);

		audio.volume = LocalStorage.getNumber('volume', 1);
		audio.muted = LocalStorage.getBoolean('muted', false);
		setVolume(audio.volume);
		setMuted(audio.muted);

		return () => {
			audio.removeEventListener('timeupdate', handleTimeTick);
			audio.removeEventListener('durationchange', handleDurationChange);
			audio.removeEventListener('ended', handleSongEnd);
		}
	}, [duration, musicContext.playedTrack ? musicContext.playedTrack.id : 0]);

	if (
		(!previousPlaying && playing) // Started playing something when we weren't playing anything
		|| (previousCurrentSessionPlayCounter !== currentSessionPlayCounter) // Song changed
	) {
		socketContext.sendPlayEvent(musicContext.playedTrack);
		lastSongPlayHeartbeatTime = Date.now();
	} else if (previousPlaying && !playing) {
		socketContext.sendPlayEvent(null);
	}

	if (musicContext.playedTrack && musicContext.sessionPlayCounter !== currentSessionPlayCounter) {
		handleSongChange();
	}

	const handleTimeTick = event => {
		const currentTime = event.target.currentTime;
		lastTime = currentTime;

		if (duration > 0) {
			// Truncate the percentage to 2 decimal places, since our progress bar only updates in 1/100 increments.
			// Doing this allows us to skip many re-renders that do nothing.
			setCurrentTimePercent(currentTime / duration);
		}

		const timeElapsed = currentTime - lastTime;
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

		broadcastListenHeartbeatIfNeeded();
	};

	const playedTrack = musicContext.playedTrack;
	const src = playedTrack ? songUrl : '';

	previousCurrentSessionPlayCounter = currentSessionPlayCounter;
	previousPlaying = playing;

	return (
		<div className="playback-controls d-flex">
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
							className="fas fa-fast-backward control"
						/>
						<i
							onMouseDown={togglePause}
							className={`fas fa-${playing ? 'pause' : 'play'} control`}
						/>
						<i
							onMouseDown={musicContext.playNext}
							className="fas fa-fast-forward control"
						/>
						<i
							onMouseDown={() => musicContext.setShuffleSongs(!musicContext.shuffleSongs)}
							className={`fas fa-random control ${musicContext.shuffleSongs ? 'enabled' : ''}`}
						/>
						<i
							onMouseDown={() => musicContext.setRepeatSongs(!musicContext.repeatSongs)}
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
							className={`fas ${getVolumeIcon()}`}
							onMouseDown={toggleMute}
						/>
						<input
							type="range"
							className="volume-slider"
							onChange={changeVolume}
							min="0"
							max="1"
							step="0.01"
							value={volume}
						/>
					</div>
				</div>
			</div>

			<div id="shuffle-wrapper">
				{ musicContext.shuffleSongs ? <ShuffleChaos/> : <div/> }
			</div>
		</div>
	)
}
