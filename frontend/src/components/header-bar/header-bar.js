import React from 'react';
import {UserButton, SongUpload} from "..";
import {YoutubeDlButton} from "../youtube-dl/youtube-dl";
import {SearchBar} from "../search-bar/search-bar";

export function HeaderBar() {
	return (
		<div className="header-bar">
			<SongUpload/>
			<YoutubeDlButton/>
			<SearchBar/>
			<div className="user-button-wrapper">
				<UserButton/>
			</div>
		</div>
	)
}
