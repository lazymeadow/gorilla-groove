import React, {useContext} from 'react';
import YoutubeDlButton from "../youtube-dl/youtube-dl";
import SearchBar from "../search-bar/search-bar";
import {UserContext} from "../../services/user-provider";
import UserButton from "../user-button/user-button";
import {SongUpload} from "..";
import Filter from "./filter/filter";
import DisconnectedWarningBar from "./disconnected-warning-bar";

export default function HeaderBar(props) {
	const userContext = useContext(UserContext);

	// noinspection HtmlUnknownTarget
	return (
		<div className="header-bar">
			<DisconnectedWarningBar/>

			<div className="d-flex">
				<img src="./images/logo.png" width="50" height="50"/>
				<div className="vertical-center">
					<div>Gorilla Groove</div>
					<div className="logo-text">Discover Edition</div>
				</div>
				<div className="song-acquisition-icons">
					<SongUpload/>
					<YoutubeDlButton/>
				</div>
			</div>

			<div className="search-content">
				<SearchBar
					centerView={props.centerView}
				/>
				<Filter/>
			</div>

			<div className="user-button-wrapper">
				{userContext.ownUser.username}
				<UserButton/>
			</div>
		</div>
	)
}
