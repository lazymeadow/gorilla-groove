import React from 'react';
import {TrackList, PlaybackControls, Api} from "..";
import {NowPlayingList} from "../now-playing-list/now-playing-list";
import {AlbumArt} from "../album-art/album-art";
import {TrackSourceList} from "../track-source-list/track-source-list";
import {HeaderBar} from "../header-bar/header-bar";
import {MusicContext} from "../../services/music-provider";

export class SiteLayout extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			isLoaded: false, // TODO use this to actually indicate loading
			ownUser: null,
			otherUsers: []
		};

		// TODO perhaps also check that this token is valid, not just that it exists
		if (!sessionStorage.getItem('token')) {
			this.props.history.push('/login'); // Redirect to the login page now that we logged out
		}
	}

	componentDidMount() {
		this.context.loadSongsForUser();
		this.context.loadPlaylists();

		Api.get("user")
			.then((result) => {
				let ownUserIndex = result.findIndex((user) => {
					return user.email.toLowerCase() === sessionStorage.getItem('loggedInEmail').toLowerCase();
				});
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
			.catch((error) => {
				console.error(error)
			});
	}

	render() {
		let displayedColumns = this.context.columnPreferences
			.filter(columnPreference => columnPreference.enabled)
			.map(columnPreference => columnPreference.name);

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
				<div id="library-view" className="border-layout-center track-list-container">
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
			</div>
		);
	}
}
SiteLayout.contextType = MusicContext;
