import React from 'react';
import {Api} from "../../api";

export function PlaybackControls(props) {
	let src = props.playedTrack ? Api.songResourceLink(props.playedTrack.track.fileName) : '';

	return (
		<div>
			Now Playing: {props.playedTrack ? props.playedTrack.track.name : 'Nothing'}
			<div>
				<button>Start</button>
				<button>Stop</button>
				<audio src={src} controls>
					Your browser is ancient. Be less ancient.
				</audio>
			</div>
		</div>
	)
}
