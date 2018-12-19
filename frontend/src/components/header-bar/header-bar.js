import React from 'react';
import {UserButton, SongUpload} from "..";
import {AddPlaylistButton} from "../add-playlist/add-playlist";

export function HeaderBar() {
	return (
		<div className="header-bar">
			<SongUpload/>
			<AddPlaylistButton/>
			<div className="user-button-wrapper">
				<UserButton/>
			</div>
		</div>
	)
}
