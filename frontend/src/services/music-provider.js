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
			forceTrackUpdate: (...args) => this.forceTrackUpdate(...args),
			playFromTrackIndex: (...args) => this.playFromTrackIndex(...args),
			playTracks: (...args) => this.playTracks(...args),
			playTracksNext: (...args) => this.playTracksNext(...args),
			playTracksLast: (...args) => this.playTracksLast(...args),
			playNext: (...args) => this.playNext(...args)
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

	playFromTrackIndex(trackIndex, updateNowPlaying) {
		this.setState({ playedTrackIndex: trackIndex });

		if (updateNowPlaying) {
			this.setState({
				nowPlayingTracks: this.state.libraryTracks.slice(0),
				playedTrack: this.state.libraryTracks[trackIndex]
			})
		} else {
			this.setState({ playedTrack: this.state.nowPlayingTracks[trackIndex] });
		}
	}

	playTracks(tracks) {
		this.setState({
			nowPlayingTracks: tracks,
			playedTrack: tracks[0],
			playedTrackIndex: 0
		})
	}

	playTracksNext(tracks) {
		// Feels kind of dirty to mutate the original then pass it in as setState
		this.state.nowPlayingTracks.splice(this.state.playedTrackIndex + 1, 0, ...tracks);
		this.setState({ nowPlayingTracks: this.state.nowPlayingTracks });
	}

	playTracksLast(tracks) {
		this.state.nowPlayingTracks.splice(this.state.nowPlayingTracks.length, 0, ...tracks);
		this.setState({ nowPlayingTracks: this.state.nowPlayingTracks });
	}

	forceTrackUpdate() {
		this.setState({
			nowPlayingTracks: this.state.nowPlayingTracks,
			libraryTracks: this.state.libraryTracks
		});
	}

	playNext() {
		let newTrackIndex = this.state.playedTrackIndex + 1;
		this.setState({
			playedTrackIndex: newTrackIndex,
			playedTrack: this.state.nowPlayingTracks[newTrackIndex]
		})
	}

	render() {
		return (
			<MusicContext.Provider value={this.state}>
				{this.props.children}
			</MusicContext.Provider>
		)
	}
}
