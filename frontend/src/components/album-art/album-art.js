import React from 'react';
import {Api} from "../../api";

export function AlbumArt(props) {

	let defaultImage = './static/unknown-art.jpg';
	let userTrack = props.playedTrackIndex ? props.nowPlayingTracks[props.playedTrackIndex] : null;
	let imgSrc = userTrack ? Api.getAlbumArtResourceLink(userTrack) : defaultImage;

	let addDefaultSrc = function(event){
		event.target.src = defaultImage;
	};

	return (
		<div className="album-art-container">
			<div className="album-art-header">Album Art</div>
			<img
				className="album-art"
				src={imgSrc}
				onError={addDefaultSrc}
			/>
		</div>
	)
}
