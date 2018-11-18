import React from 'react';
import {LibraryList, LogoutButton, PlaybackControls, SongUpload, Api} from "..";

export class LibraryLayout extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			isLoaded: false, // TODO use this to actually indicate loading
			userTracks: [],
			nowPlayingTracks: [],
			playedTrackIndex: 0
		}
	}

	componentDidMount() {
		Api.get("library").then(
			(result) => {
				this.setState({userTracks: result.content});
			},
			(error) => {
				console.error(error)
			}
		)
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
				West
			</div>
			<div className="border-layout-center track-table-container">
				<LibraryList
					userTracks={this.state.userTracks}
					playedTrackIndex={this.state.playedTrackIndex}
					playTrack={this.playTrack.bind(this)}/>
			</div>
			<div className="border-layout-east">
				East
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
