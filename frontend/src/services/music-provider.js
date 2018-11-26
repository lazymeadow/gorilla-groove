import React from "react";
import {Api} from "../api";

export const MusicContext = React.createContext();

export class MusicProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			libraryTracks: [],
			nowPlayingTracks: [],
			playedTrack: null,
			playedTrackIndex: null,
			loadSongsForUser: (...args) => this.loadSongsForUser(...args),
			playTrackIndex: (...args) => this.playTrackIndex(...args),
			forceTrackUpdate: (...args) => this.forceTrackUpdate(...args)
		}
	}

	loadSongsForUser(userId) {
		// Default to the current user if no user is requested
		let params = userId ? { userId: userId } : {};

		Api.get("library", params)
			.then((result) => {
				this.setState({ libraryTracks: result.content });
			}).catch((error) => {
			console.error(error)
		});
	}

	playTrackIndex(trackIndex, updateNowPlaying) {
		this.setState({ playedTrackIndex: trackIndex });

		if (updateNowPlaying) {
			this.setState({
				nowPlayingTracks: this.state.libraryTracks,
				playedTrack: this.state.libraryTracks[trackIndex]
			})
		} else {
			this.setState({ playedTrack: this.state.nowPlayingTracks[trackIndex] });
		}
	}

	forceTrackUpdate() {
		this.setState({
			nowPlayingTracks: this.state.nowPlayingTracks,
			libraryTracks: this.state.libraryTracks
		});
	}

	render() {
		return (
			<MusicContext.Provider value={this.state}>
				{this.props.children}
			</MusicContext.Provider>
		)
	}
}
