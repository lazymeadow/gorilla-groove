import React from 'react';
import {UserButton, SongUpload} from "..";
import {YoutubeDlButton} from "../youtube-dl/youtube-dl";
import {SearchBar} from "../search-bar/search-bar";
import {getCookieValue} from "../../cookie";

export function HeaderBar() {
	return (
		<div className="header-bar">
			<div className="display-flex">
				<img src="./images/logo.png" width="50" height="50"/>
				<div className="vertical-center">
					<div>Gorilla</div>
					<div>Groove</div>
				</div>
				<div className="song-acquisition-icons">
					<SongUpload/>
					<YoutubeDlButton/>
				</div>
			</div>

			<SearchBar/>
			<div className="user-button-wrapper">
				{getCookieValue('loggedInUserName')}
				<UserButton/>
			</div>
		</div>
	)
}
