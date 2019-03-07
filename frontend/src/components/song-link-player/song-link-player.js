import React from 'react';
import {Api} from "..";
import {MusicContext} from "../../services/music-provider";

export class SongLinkPlayer extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			songLink: null
		}
	}

	componentDidUpdate() {
		document.getElementById('song-link-player').play();
	}

	componentDidMount() {
		Api.get('file/track-link/' + this.props.match.params.trackId).then((res) => {
			this.setState({ songLink: res.songLink });
		})
	}

	render() {
		return (
			<div id="song-link-player">
				<audio src={this.state.songLink} controls>
					Your browser is ancient. Use a better browser
				</audio>
			</div>
		);
	}
}
