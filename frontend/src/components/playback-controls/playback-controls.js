import React from 'react';
import {Api} from "../../api";

export class PlaybackControls extends React.Component {
	constructor(props) {
		super(props);
	}

	componentDidUpdate() {
		// Start playing the new song
		if (this.props.playedTrack) {
			document.getElementById('audio').play();
		}
	}

	render() {
		let src = this.props.playedTrack ? Api.songResourceLink(this.props.playedTrack.track.fileName) : '';
		return (
			<div>
				Now Playing: {this.props.playedTrack ? this.props.playedTrack.track.name : 'Nothing'}
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
