import React from 'react';
import {MusicContext} from "../../services/music-provider";

export class AddPlaylistButton extends React.Component {
	constructor(props) {
		super(props);
	}

	render() {
		return (
			<i className="fas fa-plus add-playlist-button" onClick={(e) => {
				e.stopPropagation();
				this.context.createPlaylist(e)
			}}/>
		)
	}
}
AddPlaylistButton.contextType = MusicContext;
