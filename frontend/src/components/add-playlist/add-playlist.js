import React from 'react';
import {Api} from "../../api";

export class AddPlaylistButton extends React.Component {
	constructor(props) {
		super(props);
	}

	createPlaylist() {
		Api.post('playlist', {name: 'New Playlist'})
			.then(result => console.log(result))
			.catch(error => console.error(error));
	}

	render() {
		return (
			<button className="add-playlist-button" onClick={() => this.createPlaylist()}>
				Create a playlist
			</button>
		)
	}
}
