import React, {useContext} from 'react';
import {PlaylistContext} from "../../services/playlist-provider";

export default function AddPlaylistButton() {
	const playlistContext = useContext(PlaylistContext);

	return (
		<i className="fas fa-plus add-playlist-button" onMouseDown={(e) => {
			e.stopPropagation();
			playlistContext.createPlaylist(e)
		}}/>
	)
}
