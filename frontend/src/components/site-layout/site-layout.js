import React from 'react';
import {TrackList, PlaybackControls, Api} from "..";
import {NowPlayingList} from "../now-playing-list/now-playing-list";
import {AlbumArt} from "../album-art/album-art";
import {TrackSourceList} from "../track-source-list/track-source-list";
import {HeaderBar} from "../header-bar/header-bar";
import {MusicContext} from "../../services/music-provider";
import {SiteStats} from "../site-stats/site-stats";
import {isLoggedIn} from "../../util";
import {getCookieValue} from "../../cookie";
import {notifyVersion} from "../../services/version";
import {PermissionType} from "../../enums/permission-type";

export class SiteLayout extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			isLoaded: false, // TODO use this to actually indicate loading
			ownUser: null,
			ownPermissions: new Set(),
			otherUsers: []
		};

		if (!isLoggedIn()) {
			this.props.history.push('/login'); // Redirect to the login page now that we logged out
		}
	}

	componentDidMount() {
		if (!isLoggedIn()) {
			console.info('Not logged in. Awaiting redirect');
			return;
		}

		this.context.connectToSocket();
		this.context.loadSongsForUser();
		this.context.loadPlaylists();

		notifyVersion();

		Api.get('user/permissions').then(result => {
			const permissionSet = new Set(result.map(it => PermissionType[it.permissionType]));
			this.context.setProviderState({ ownPermissions: permissionSet });
		});

		Api.get('user', { showAll: false })
			.then(result => {
				const loggedInEmail = getCookieValue('loggedInEmail').toLowerCase();
				const ownUserIndex = result.findIndex(user => user.email.toLowerCase() === loggedInEmail);

				let ownUser = null;
				if (ownUserIndex === -1) {
					console.error("Could not locate own user within Gorilla Groove's users");
					ownUser = result[0];
				} else {
					ownUser = result.splice(ownUserIndex, 1)[0];
				}

				this.setState({
					ownUser: ownUser,
					otherUsers: result
				})
			})
			.catch(error => {
				console.error(error);
				// If we had an error here it PROBABLY means we had a failure to login
				this.props.history.push('/login');
			});
	}

	render() {
		if (!isLoggedIn()) {
			// Just do nothing until the redirect catches up and takes us to the login page
			return <div/>
		}

		const displayedColumns = this.context.columnPreferences
			.filter(columnPreference => columnPreference.enabled)
			.map(columnPreference => columnPreference.name);

		console.log('Render parent');

		return (
			<div className="full-screen border-layout">
				<div className="border-layout-north">
					<HeaderBar/>
				</div>
				<div className="border-layout-west">
					<TrackSourceList
						ownUser={this.state.ownUser}
						otherUsers={this.state.otherUsers}
						playlists={this.context.playlists}
					/>
				</div>
				<div id="library-view" className="border-layout-center track-list-container p-relative">
					<TrackList
						columns={displayedColumns}
						userTracks={this.context.viewedTracks}
						trackView={true}
					/>
				</div>
				<div className="border-layout-east track-list-container">
					<NowPlayingList
						columns={["#", "Name"]}
						userTracks={this.context.nowPlayingTracks}
						trackView={false}
					/>
				</div>
				<div className="border-layout-southwest">
					<AlbumArt/>
				</div>
				<div className="border-layout-south">
					<PlaybackControls/>
				</div>
				<div className="border-layout-southeast">
					<SiteStats/>
				</div>
			</div>
		);
	}
}
SiteLayout.contextType = MusicContext;
