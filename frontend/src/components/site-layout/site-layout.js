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
		}
	}

	componentDidMount() {
		this.context.loadSongsForUser();
		this.context.loadPlaylists();

		Api.get("user")
			.then((result) => {
				let ownUserIndex = result.findIndex((user) => {
					return user.email === sessionStorage.getItem('loggedInEmail');
				});
				let ownUser = result.splice(ownUserIndex, 1)[0];

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
					<AlbumArt/>
				</div>
				<div className="border-layout-center track-table-container">
					<TrackList
						columns={["Name", "Artist", "Album", "Length", "Year", "Play Count", "Bit Rate", "Sample Rate", "Added", "Last Played"]}
						userTracks={this.context.viewedTracks}
						trackView={true}
					/>
				</div>
				<div className="border-layout-east track-table-container">
					<NowPlayingList
						columns={["#", "Name"]}
						userTracks={this.context.nowPlayingTracks}
						trackView={false}
					/>
				</div>
				<div className="border-layout-south">
					<PlaybackControls/>
				</div>
			</div>
		);
	}
}
SiteLayout.contextType = MusicContext;
