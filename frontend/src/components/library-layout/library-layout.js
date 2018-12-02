import React from 'react';
import {LibraryList, PlaybackControls, Api} from "..";
import {NowPlayingList} from "../now-playing-list/now-playing-list";
import {AlbumArt} from "../album-art/album-art";
import {TrackSourceList} from "../track-source-list/track-source-list";
import {HeaderBar} from "../header-bar/header-bar";
import {MusicContext} from "../../services/music-provider";
import {Modal} from "../modal/modal";

export class LibraryLayout extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			isLoaded: false, // TODO use this to actually indicate loading
			users: []
		}
	}

	componentDidMount() {
		this.context.loadSongsForUser(null);

		Api.get("user")
			.then((result) => {
				let usersWithoutSelf = result.filter((user) => {
					return user.email !== sessionStorage.getItem('loggedInEmail');
				});
				this.setState({ users: usersWithoutSelf })
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
					<TrackSourceList users={this.state.users}/>
					<AlbumArt/>
				</div>
				<div className="border-layout-center track-table-container">
					<LibraryList
						columns={["Name", "Artist", "Album", "Length", "Year", "Play Count", "Bit Rate", "Sample Rate", "Added", "Last Played"]}
						userTracks={this.context.libraryTracks}
						updateNowPlaying={true}
					/>
				</div>
				<div className="border-layout-east track-table-container">
					<NowPlayingList
						columns={["#", "Name"]}
						userTracks={this.context.nowPlayingTracks}
						updateNowPlaying={false}
					/>
				</div>
				<div className="border-layout-south">
					<PlaybackControls/>
				</div>
			</div>
		);
	}
}
LibraryLayout.contextType = MusicContext;
