import React, {useEffect, useRef, useState, useContext} from "react";
import MiniPlayer from "../../playback-controls/mini-player/mini-player";
import {MusicFilterContext} from "../../../services/music-filter-provider";
import {Api} from "../../../api";

export default function SpotifySearch(props) {
	const [spotifyTracks, setSpotifyTracks] = useState([]);

	const musicFilterContext = useContext(MusicFilterContext);

	useEffect(() => {
		if (musicFilterContext.searchTerm.length > 0) {
			Api.get('search/spotify/artist/' + musicFilterContext.searchTerm).then(tracks => {
				setSpotifyTracks(tracks);
				console.log(tracks);
			})
		}
	}, [musicFilterContext.searchTerm]);

	return (
		<div id="spotify-search">
			{ spotifyTracks.map(track =>
				<MiniPlayer
					key={track.sourceId}
					trackData={track}
					playing={false}
					timePlayed={0}
					onTimeChange={(newTimePercent, isHeld) => {
						// if (!isHeld) {
						// 	return;
						// }
						//
						// const newTime = newTimePercent * trackData.length;
						// setTimePlayed(newTime);
						// audioRef.current.currentTime = newTime;
					}}
					onPauseChange={() => {
						// if (playing) {
						// 	audioRef.current.pause();
						// } else {
						// 	audioRef.current.play();
						// }
						//
						// setPlaying(!playing);
					}}
					volume={1}
					onVolumeChange={newVolume => {
						// setVolume(newVolume);
						// audioRef.current.volume = newVolume;
					}}
					muted={false}
					onMuteChange={() => {
						// audioRef.current.muted = !muted;
						// setMuted(!muted);
					}}
				/>
			)}
			Spotify Search
		</div>
	)
}
