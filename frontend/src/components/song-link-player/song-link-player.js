import React from 'react';
import {Api} from "..";

export class SongLinkPlayer extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			songLink: null
		}
	}

	componentDidMount() {
		Api.get('file/track-link/' + this.props.match.params.trackId).then((res) => {
			this.setState({ songLink: res.songLink }, () => {
				document.getElementById('audio-player').play();
			});
		})
	}

	render() {
		return (
			<div id="song-link-player">
				<audio id="audio-player" src={this.state.songLink} controls>
					Your browser is ancient. Use a better browser
				</audio>
			</div>
		);
	}
}
