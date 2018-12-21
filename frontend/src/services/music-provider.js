import React from "react";
import {Api} from "../api";
import {TrackView} from "../enums/TrackView";

export const MusicContext = React.createContext();

export class MusicProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			viewedTracks: [],
			trackView: TrackView.LIBRARY,
			viewedEntityId: null, // An ID for the user or library being viewed, or null if viewing the user's own library
			trackSortColumn: 'Artist',
			trackSortDir: 'asc',
			nowPlayingTracks: [],
			playedTrack: null,
			playedTrackIndex: null,
			playlists: [],
			loadSongsForUser: (...args) => this.loadSongsForUser(...args),
			sortTracks: (...args) => this.sortTracks(...args),
			forceTrackUpdate: (...args) => this.forceTrackUpdate(...args),
			playFromTrackIndex: (...args) => this.playFromTrackIndex(...args),
			playTracks: (...args) => this.playTracks(...args),
			playTracksNext: (...args) => this.playTracksNext(...args),
			playTracksLast: (...args) => this.playTracksLast(...args),
			playNext: (...args) => this.playNext(...args),
			setHidden: (...args) => this.setHidden(...args),
			loadPlaylists: (...args) => this.loadPlaylists(...args),
			loadSongsForPlaylist: (...args) => this.loadSongsForPlaylist(...args),
			addToPlaylist: (...args) => this.addToPlaylist(...args),
			removeFromPlaylist: (...args) => this.removeFromPlaylist(...args)
		};

		this.trackKeyConversions = {
			'Name': 'name',
			'Artist': 'artist',
			'Album': 'album',
			'Length': 'length',
			'Year': 'releaseYear',
			'Play Count': 'playCount',
			'Bit Rate': 'bitRate',
			'Sample Rate': 'sampleRate',
			'Added': 'createdAt',
			'Last Played': 'lastPlayed',
		};
	}

	loadSongsForUser(userId, params) {
		params = params ? params : {};
		// If userId is null, the backend uses the current user
		if (userId) {
			params.userId = userId;
			this.setState({
				trackView: TrackView.USER,
				viewedEntityId: userId
			});
		} else {
			this.setState({
				trackView: TrackView.LIBRARY,
				viewedEntityId: null
			});
		}

		Api.get("track", params).then((result) => {
			this.setState({ viewedTracks: result.content });
		}).catch((error) => {
			console.error(error)
		});
	}

	sortTracks(sortColumn, sortDir) {
		let params = {};

		if (sortColumn && sortDir) {
			params.sort = `${this.trackKeyConversions[sortColumn]},${sortDir}`;

			this.setState({
				trackSortColumn: sortColumn,
				trackSortDir: sortDir
			});
		} else {
			params.sort = `${this.trackKeyConversions[this.state.trackSortColumn]},${this.state.trackSortDir}`
		}

		// Messing around with the JPA sorting setup is more hassle than it is worth
		// For sorting playlists, just append 'track.' in front so the key is correct for playlist tracks
		if (this.state.trackView === TrackView.PLAYLIST) {
			params.sort = `track.${params.sort}`;
		}

		if (this.state.trackView === TrackView.USER || this.state.trackView === TrackView.LIBRARY) {
			this.loadSongsForUser(this.state.viewedEntityId, params);
		} else if (this.state.trackView === TrackView.PLAYLIST) {
			this.loadSongsForPlaylist(this.state.viewedEntityId, params);
		}
	}

	playFromTrackIndex(trackIndex, updateNowPlaying) {
		this.setState({ playedTrackIndex: trackIndex });

		if (updateNowPlaying) {
			this.setState({
				nowPlayingTracks: this.state.viewedTracks.slice(0),
				playedTrack: this.state.viewedTracks[trackIndex]
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
			viewedTracks: this.state.viewedTracks
		});
	}

	playNext() {
		let newTrackIndex = this.state.playedTrackIndex + 1;
		this.setState({
			playedTrackIndex: newTrackIndex,
			playedTrack: this.state.nowPlayingTracks[newTrackIndex]
		})
	}

	setHidden(tracks, isHidden) {
		Api.post('track/set-hidden', {
			trackIds: tracks.map(track => track.id),
			isHidden: isHidden
		}).then(() => {
			tracks.forEach(track => track.hidden = isHidden);
			this.forceTrackUpdate();
		}).catch(error => {
			console.error(error);
		});
	}

	loadPlaylists() {
		Api.get('playlist').then((playlists) => {
			this.setState({playlists: playlists});
		})
	}

	loadSongsForPlaylist(playlistId, params) {
		params = params ? params : {};
		params.playlistId = playlistId;

		this.setState({
			trackView: TrackView.PLAYLIST,
			viewedEntityId: playlistId
		});

		Api.get('playlist/track', params).then((result) => {
			this.setState({ viewedTracks: result.content });
		})
	}

	addToPlaylist(playlistId, trackIds) {
		Api.post("playlist/track", {
			playlistId: playlistId,
			trackIds: trackIds
		}).then(() => {
			console.log("Wow, Ayrton. Great moves. Keep it up. I'm proud of you.");
		})
	}

	removeFromPlaylist(trackIds, playlistId) {

	}

	render() {
		return (
			<MusicContext.Provider value={this.state}>
				{this.props.children}
			</MusicContext.Provider>
		)
	}
}
