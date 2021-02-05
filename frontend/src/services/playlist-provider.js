import React from "react";
import {Api} from "../api";
import {toast} from "react-toastify";

export const PlaylistContext = React.createContext();

export class PlaylistProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			playlists: [],

			loadPlaylists: (...args) => this.loadPlaylists(...args),
			renamePlaylist: (...args) => this.renamePlaylist(...args),
			deletePlaylist: (...args) => this.deletePlaylist(...args),
			createPlaylist: (...args) => this.createPlaylist(...args),
			addToPlaylist: (...args) => this.addToPlaylist(...args),
			removeFromPlaylist: (...args) => this.removeFromPlaylist(...args),
		}
	}

	loadPlaylists() {
		return Api.get('playlist').then(playlists => {
			this.setState({ playlists });
		})
	}

	deletePlaylist(playlist) {
		const deleteId = playlist.id;
		return Api.delete(`playlist/${deleteId}`).then(() => {
			const newPlaylists = this.state.playlists.filter(playlist => playlist.id !== deleteId);
			this.setState({ playlists: newPlaylists });
		})
	}

	createPlaylist() {
		Api.post('playlist', {name: 'New Playlist'})
			.then(playlist => {
				const playlists = this.state.playlists.slice(0);
				playlists.push(playlist);

				this.setState({ playlists });

				toast.success('New playlist created')
			})
			.catch(error => {
				console.error(error);
				toast.error('The creation of a new playlist failed');
			});
	}

	renamePlaylist(playlist, newName) {
		playlist.name = newName;

		this.setState({ playlists: this.state.playlists });

		return Api.put(`playlist/${playlist.id}`, { name: newName }).catch(error => {
			console.error(error);
			toast.error("Failed to updated playlist name")
		})
	}

	addToPlaylist(playlistId, trackIds) {
		return Api.post('playlist/track', {
			playlistIds: [playlistId],
			trackIds: trackIds
		}).then(() => {
			console.log("Wow, Ayrton. Great moves. Keep it up. I'm proud of you.");
		})
	}

	removeFromPlaylist(playlistTrackIds) {
		return Api.delete('playlist/track', {
			playlistTrackIds: playlistTrackIds
		});
	}

	render() {
		return (
			<PlaylistContext.Provider value={this.state}>
				{this.props.children}
			</PlaylistContext.Provider>
		)
	}
}
