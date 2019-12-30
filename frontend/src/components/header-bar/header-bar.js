import React, {useContext} from 'react';
import {YoutubeDlButton} from "../youtube-dl/youtube-dl";
import SearchBar from "../search-bar/search-bar";
import {UserContext} from "../../services/user-provider";
import UserButton from "../user-button/user-button";
import {SongUpload} from "..";

export default function HeaderBar() {
	const userContext = useContext(UserContext);

	return (
		<div className="header-bar">
			<div className="d-flex">
				<img src="./images/logo.png" width="50" height="50"/>
				<div className="vertical-center">
					<div>Gorilla Groove</div>
					<div className="quantum">Quantum</div>
				</div>
				<div className="song-acquisition-icons">
					<SongUpload/>
					<YoutubeDlButton/>
				</div>
			</div>

			<SearchBar/>
			<div className="user-button-wrapper">
				{userContext.ownUser.username}
				<UserButton/>
			</div>
		</div>
	)
}

