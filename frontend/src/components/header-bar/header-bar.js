import React from 'react';
import {UserButton, SongUpload} from "..";
import {AddPlaylistButton} from "../add-playlist/add-playlist";
import {InviteUserButton} from "../invite-user/invite-user";
import {YoutubeDlButton} from "../youtube-dl/youtube-dl";

export function HeaderBar() {
	return (
		<div className="header-bar">
			<SongUpload/>
			<AddPlaylistButton/>
			<InviteUserButton/>
			<YoutubeDlButton/>
			<div className="user-button-wrapper">
				<UserButton/>
			</div>
		</div>
	)
}
