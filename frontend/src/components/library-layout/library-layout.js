import React from 'react';
import {LibraryList, LogoutButton, PlaybackControls, SongUpload, Api} from "..";
import {NowPlayingList} from "../now-playing-list/now-playing-list";
import {AlbumArt} from "../album-art/album-art";
import {TrackSourceList} from "../track-source-list/track-source-list";

export class LibraryLayout extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			isLoaded: false, // TODO use this to actually indicate loading
			userTracks: [],
			nowPlayingTracks: [],
			playedTrackIndex: null,
			users: []
		}
	}

	componentDidMount() {
		this.loadSongsForUser(null);

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

	loadSongsForUser(userId) {
		// Default to the current user if no user is requested
		let params = userId ? { userId: userId } : {};

		Api.get("library", params)
			.then((result) => {
				this.setState({ userTracks: result.content });
			}).catch((error) => {
			console.error(error)
		});
	}

	playTrack(trackIndex) {
		this.setState({
			nowPlayingTracks: this.state.userTracks,
			playedTrackIndex: trackIndex
		});
	}

	render() {
		return <div className="full-screen border-layout">
			<div className="border-layout-north">
				<LogoutButton/>
				<SongUpload/>
			</div>
			<div className="border-layout-west">
				<TrackSourceList
					users={this.state.users}
					loadSongs={this.loadSongsForUser.bind(this)}
				/>
				<AlbumArt
					nowPlayingTracks={this.state.nowPlayingTracks}
					playedTrackIndex={this.state.playedTrackIndex}
				/>
			</div>
			<div className="border-layout-center track-table-container">
				<LibraryList
					columns={["Name", "Artist", "Album", "Length", "Year", "Play Count", "Bit Rate", "Sample Rate", "Added", "Last Played"]}
					userTracks={this.state.userTracks}
					playedTrackIndex={this.state.playedTrackIndex}
					playTrack={this.playTrack.bind(this)}/>
			</div>
			<div className="border-layout-east track-table-container">
				<NowPlayingList
					columns={["#", "Name"]}
					userTracks={this.state.nowPlayingTracks}
					playedTrackIndex={this.state.playedTrackIndex}
					playTrack={this.playTrack.bind(this)}/>
			</div>
			<div className="border-layout-south">
				<PlaybackControls
					nowPlayingTracks={this.state.nowPlayingTracks}
					playedTrackIndex={this.state.playedTrackIndex}
				/>
			</div>
		</div>;
	}
}
