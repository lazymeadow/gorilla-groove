import React from 'react';
import {MusicContext} from "../../services/music-provider";

export class AddPlaylistButton extends React.Component {
	constructor(props) {
		super(props);
	}

	render() {
		return (
			<button className="add-playlist-button" onClick={() => this.context.createPlaylist()}>
				Create a playlist
			</button>
		)
	}
}
AddPlaylistButton.contextType = MusicContext;
