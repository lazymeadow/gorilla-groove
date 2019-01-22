import React from 'react';
import {UserButton, SongUpload} from "..";
import {AddPlaylistButton} from "../add-playlist/add-playlist";
import {InviteUserButton} from "../invite-user/invite-user";

export function HeaderBar() {
	return (
		<div className="header-bar">
			<SongUpload/>
			<AddPlaylistButton/>
			<InviteUserButton/>
			<div className="user-button-wrapper">
				<UserButton/>
			</div>
		</div>
	)
}
