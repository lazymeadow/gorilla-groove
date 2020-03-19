import React, {useEffect, useRef, useState} from 'react';
import {Api} from "..";
import MiniPlayer from "../playback-controls/mini-player/mini-player";

export default function SongLinkPlayer(props) {
	const audioRef = useRef(null);

	const [trackData, setTrackData] = useState({});

	const [volume, setVolume] = useState(1);
	const [muted, setMuted] = useState(false);
	const [playing, setPlaying] = useState(false);
	const [timePlayed, setTimePlayed] = useState(false);

	useEffect(() => {
		Api.get('track/public/' + props.match.params.trackId).then(res => {
			setTrackData(res);
		});

		const audio = audioRef.current;
		audio.addEventListener('timeupdate', event => {
			setTimePlayed(event.target.currentTime);
		});
		audio.addEventListener('ended', () => {
			setPlaying(false);
		});
	}, []);

	return (
		<div id="song-link-player">
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
