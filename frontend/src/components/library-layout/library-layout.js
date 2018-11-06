import React from 'react';
import {LibraryList, LogoutButton, PlaybackControls, SongUpload, Api} from "..";

export class LibraryLayout extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			isLoaded: false, // TODO use this to actually indicate loading
			userTracks: [],
			playedTrack: null
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

	playTrack(userTrack) {
		this.setState({playedTrack: userTrack});
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
			<div className="border-layout-center">
				<LibraryList userTracks={this.state.userTracks} playSong={this.playTrack.bind(this)}/>
			</div>
			<div className="border-layout-east">
				East
			</div>
			<div className="border-layout-south">
				<PlaybackControls playedTrack={this.state.playedTrack}/>
			</div>
		</div>;
	}
}
