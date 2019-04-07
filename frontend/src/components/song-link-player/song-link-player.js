import React from 'react';
import {Api} from "..";

export class SongLinkPlayer extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			trackData: {}
		}
	}

	componentDidMount() {
		Api.get('track/public/' + this.props.match.params.trackId).then(res => {
			this.setState({ trackData: res }, () => {
				document.getElementById('audio-player').play();
			})
		});
	}

	getDisplayedSongName() {
		const playedTrack = this.state.trackData;
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
	}

	render() {
		return (
			<div id="song-link-player">
				<img src={this.state.trackData.albumLink}/>
				<div>{this.getDisplayedSongName()}</div>
				<audio id="audio-player" src={this.state.trackData.trackLink} controls>
					Your browser is ancient. Use a better browser
				</audio>
			</div>
		);
	}
}
