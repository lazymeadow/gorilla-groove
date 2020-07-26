import React, {useEffect, useRef, useState} from 'react';
import {Api} from "..";
import MiniPlayer from "../playback-controls/mini-player/mini-player";
import {isLoggedIn} from "../../util";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";

export default function SongLinkPlayer(props) {
	const audioRef = useRef(null);

	const [loading, setLoading] = useState(true);
	const [error, setError] = useState(null);
	const [trackData, setTrackData] = useState({});

	const [volume, setVolume] = useState(1);
	const [muted, setMuted] = useState(false);
	const [playing, setPlaying] = useState(false);
	const [timePlayed, setTimePlayed] = useState(false);

	useEffect(() => {
		// If we are logged in, we can request a different endpoint that can link to expired tracks
		const baseUrl = isLoggedIn() ? 'track/' : 'track/public/';
		Api.get(baseUrl + props.match.params.trackId).then(res => {
			setTrackData(res);
		}).catch(e => {
			if (e.status === 401) {
				setError(<div>This track link has expired. <a href="/login">Log in</a> or re-request the link</div>);
			} else if (e.status === 404) {
				setError(<div>The track was not found</div>);
			} else {
				setError(<div>An unknown error has occurred</div>);
				console.error(e);
			}
		}).finally(() => {
			setLoading(false);
		});

		const audio = audioRef.current;
		audio.addEventListener('timeupdate', event => {
			setTimePlayed(event.target.currentTime);
		});
		audio.addEventListener('ended', () => {
			setPlaying(false);
		});
	}, []);

	if (error !== null) {
		return <div id="song-link-player">{error}</div>
	}

	return (
		<div id="song-link-player" className="d-relative">
			<LoadingSpinner visible={loading}/>
			<audio ref={audioRef} src={trackData.trackLink}/>
			<MiniPlayer
				trackData={trackData}
				playing={playing}
				timePlayed={timePlayed}
				onTimeChange={(newTimePercent, isHeld) => {
					if (!isHeld) {
						return;
					}

					const newTime = newTimePercent * trackData.length;
					setTimePlayed(newTime);
					audioRef.current.currentTime = newTime;
				}}
				onPauseChange={() => {
					if (playing) {
						audioRef.current.pause();
					} else {
						audioRef.current.play();
					}

					setPlaying(!playing);
				}}
				volume={volume}
				onVolumeChange={newVolume => {
					setVolume(newVolume);
					audioRef.current.volume = newVolume;
				}}
				muted={muted}
				onMuteChange={() => {
					audioRef.current.muted = !muted;
					setMuted(!muted);
				}}
			/>
		</div>
	);
}
