import React from 'react';
import {Api} from "../../api";

export class PlaybackControls extends React.Component {
	constructor(props) {
		super(props);
	}

	componentDidUpdate() {
		// Start playing the new song
		if (this.props.playedTrackIndex) {
			document.getElementById('audio').play();
		}
	}

	render() {
		let playedTrack = this.props.playedTrackIndex ? this.props.nowPlayingTracks[this.props.playedTrackIndex] : null;
		let src = playedTrack ? Api.songResourceLink(playedTrack.track.fileName) : '';
		return (
			<div>
				Now Playing: {playedTrack ? playedTrack.track.name : 'Nothing'}
				<div>
					<button>Start</button>
					<button>Stop</button>
					<audio id="audio" src={src} controls>
						Your browser is ancient. Be less ancient.
					</audio>
				</div>
			</div>
		)
	}
}
