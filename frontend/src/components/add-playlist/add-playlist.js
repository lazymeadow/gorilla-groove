import React from 'react';
import {Api} from "../../api";
import {toast} from "react-toastify";

export class AddPlaylistButton extends React.Component {
	constructor(props) {
		super(props);
	}

	createPlaylist() {
		Api.post('playlist', {name: 'New Playlist'})
			.then(() => toast.success('New playlist created'))
			.catch(error => {
				console.error(error);
				toast.error('The creation of a new playlist failed');
			});
	}

	render() {
		return (
			<button className="add-playlist-button" onClick={() => this.createPlaylist()}>
				Create a playlist
			</button>
		)
	}
}
