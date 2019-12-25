import React, {useContext, useEffect, useState} from 'react';
import {TrackList, Api} from "..";
import {NowPlayingList} from "../now-playing-list/now-playing-list";
import {AlbumArt} from "../album-art/album-art";
import TrackSourceList from "../track-source-list/track-source-list";
import {HeaderBar} from "../header-bar/header-bar";
import {MusicContext} from "../../services/music-provider";
import {SiteStats} from "../site-stats/site-stats";
import {isLoggedIn} from "../../util";
import {getCookieValue} from "../../cookie";
import {notifyVersion} from "../../services/version";
import {PermissionType} from "../../enums/permission-type";
import {SocketContext} from "../../services/socket-provider";
import PlaybackControls from "../playback-controls/playback-controls";

export default function SiteLayout(props) {
	const [ownUser, setOwnUser] = useState(null);
	const [otherUsers, setOtherUsers] = useState([]);
	const [albumArtLink, setAlbumArtLink] = useState(null); // FIXME Really not sure where this should live long term

	if (!isLoggedIn()) {
		console.info('Not logged in. Awaiting redirect');
		props.history.push('/login'); // Redirect to the login page now that we logged out

		return <div/>;
	}

	const songContext = useContext(MusicContext);
	const socketContext = useContext(SocketContext);

	useEffect(() => {
		socketContext.connectToSocket();
		songContext.loadSongsForUser();
		songContext.loadPlaylists();

		notifyVersion();

		Api.get('user/permissions').then(result => {
			const permissionSet = new Set(result.map(it => PermissionType[it.permissionType]));
			songContext.setProviderState({
				ownPermissions: permissionSet,
				renderCounter: songContext.renderCounter + 1
			});
		});

		Api.get('user', { showAll: false }).then(result => {
			const loggedInEmail = getCookieValue('loggedInEmail').toLowerCase();
			const ownUserIndex = result.findIndex(user => user.email.toLowerCase() === loggedInEmail);

			let ownUser = null;
			if (ownUserIndex === -1) {
				console.error("Could not locate own user within Gorilla Groove's users");
				ownUser = result[0];
			} else {
				ownUser = result.splice(ownUserIndex, 1)[0];
			}

			setOwnUser(ownUser);
			setOtherUsers(result);
		}).catch(error => {
			console.error(error);
			// If we had an error here it PROBABLY means we had a failure to login
			props.history.push('/login');
		});
	}, []);

	const displayedColumns = songContext.columnPreferences
		.filter(columnPreference => columnPreference.enabled)
		.map(columnPreference => columnPreference.name);

	return (
		<div className="full-screen border-layout">
			<div className="border-layout-north">
				<HeaderBar/>
			</div>
			<div className="border-layout-west">
				<TrackSourceList
					ownUser={ownUser}
					otherUsers={otherUsers}
					playlists={songContext.playlists}
				/>
			</div>
			<div id="library-view" className="border-layout-center track-list-container p-relative">
				<TrackList
					columns={displayedColumns}
					userTracks={songContext.viewedTracks}
					trackView={true}
				/>
			</div>
			<div className="border-layout-east track-list-container">
				<NowPlayingList
					columns={["#", "Name"]}
					userTracks={songContext.nowPlayingTracks}
					trackView={false}
				/>
			</div>
			<div className="border-layout-southwest">
				<AlbumArt artLink={albumArtLink}/>
			</div>
			<div className="border-layout-south">
				<PlaybackControls setAlbumArt={setAlbumArtLink}/>
			</div>
			<div className="border-layout-southeast">
				<SiteStats/>
			</div>
		</div>
	)
}
