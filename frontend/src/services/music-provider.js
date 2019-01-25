import React from "react";
import {Api} from "../api";
import {TrackView} from "../enums/TrackView";
import * as LocalStorage from "../local-storage";
import * as Util from "../util";

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
			songIndexesToShuffle: [],
			shuffleSongs: LocalStorage.getBoolean('shuffleSongs', false),
			repeatSongs: LocalStorage.getBoolean('repeatSongs', false),
			loadSongsForUser: (...args) => this.loadSongsForUser(...args),
			sortTracks: (...args) => this.sortTracks(...args),
			forceTrackUpdate: (...args) => this.forceTrackUpdate(...args),
			playFromTrackIndex: (...args) => this.playFromTrackIndex(...args),
			playTracks: (...args) => this.playTracks(...args),
			playTracksNext: (...args) => this.playTracksNext(...args),
			playTracksLast: (...args) => this.playTracksLast(...args),
			playIndex: (...args) => this.playIndex(...args),
			deleteTracks: (...args) => this.deleteTracks(...args),
			setHidden: (...args) => this.setHidden(...args),
			loadPlaylists: (...args) => this.loadPlaylists(...args),
			loadSongsForPlaylist: (...args) => this.loadSongsForPlaylist(...args),
			addToPlaylist: (...args) => this.addToPlaylist(...args),
			removeFromPlaylist: (...args) => this.removeFromPlaylist(...args),
			updateTrack: (...args) => this.updateTrack(...args),
			renamePlaylist: (...args) => this.renamePlaylist(...args),
			setRepeatSongs: (...args) => this.setRepeatSongs(...args),
			setShuffleSongs: (...args) => this.setShuffleSongs(...args),
			resetShuffleIndexes: (...args) => this.resetShuffleIndexes(...args)
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

		this.pageSize = 50;
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

		if (!params.sort) {
			params.sort = `${this.trackKeyConversions[this.state.trackSortColumn]},${this.state.trackSortDir}`
		}

		params.size = this.pageSize;

		return Api.get("track", params).then((result) => {
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
			}, () => this.resetShuffleIndexes(trackIndex))
		} else {
			this.setState({
					playedTrack: this.state.nowPlayingTracks[trackIndex]
				}, () => this.resetShuffleIndexes(trackIndex));
		}
	}

	playTracks(tracks) {
		let startIndex = this.state.shuffleSongs ? Math.floor(Math.random() * tracks.length) : 0;
		this.setState({
			nowPlayingTracks: tracks,
			playedTrack: tracks[startIndex],
			playedTrackIndex: startIndex
		}, () => this.resetShuffleIndexes(startIndex));
	}

	playTracksNext(tracks) {
		let newTracks = this.state.nowPlayingTracks.slice(0);
		newTracks.splice(this.state.playedTrackIndex + 1, 0, ...tracks);

		this.setState({ nowPlayingTracks: newTracks });

		this.addTrackIndexesToShuffle(this.state.playedTrackIndex, tracks.length);
	}

	playTracksLast(tracks) {
		let newTracks = this.state.nowPlayingTracks.slice(0);
		newTracks.splice(this.state.nowPlayingTracks.length, 0, ...tracks);

		this.setState({ nowPlayingTracks: newTracks });

		this.addTrackIndexesToShuffle(this.state.nowPlayingTracks.length - 1, tracks.length);
	}

	forceTrackUpdate() {
		this.setState({
			nowPlayingTracks: this.state.nowPlayingTracks,
			viewedTracks: this.state.viewedTracks
		});
	}

	// newTrackIndex is the song index in the now playing list
	playIndex(newTrackIndex) {
		// If we're shuffling, we need to remove this song from the shuffle pool after we play it
		if (this.state.shuffleSongs) {
			// Couldn't resist this horrible variable name
			let indexIndex = this.state.songIndexesToShuffle.findIndex((index) => index === newTrackIndex);

			// Now that we know where the song index is, in our array of indexes we can still pick, remove the indexIndex
			let newShuffleIndexes = this.state.songIndexesToShuffle.slice(0);
			newShuffleIndexes.splice(indexIndex, 1);

			this.setState({ songIndexesToShuffle: newShuffleIndexes });
		}

		this.setState({
			playedTrackIndex: newTrackIndex,
			playedTrack: this.state.nowPlayingTracks[newTrackIndex]
		})
	}

	setHidden(tracks, isHidden) {
		return Api.post('track/set-hidden', {
			trackIds: tracks.map(track => track.id),
			isHidden: isHidden
		}).then(() => {
			tracks.forEach(track => track.hidden = isHidden);
			this.forceTrackUpdate();
		});
	}

	deleteTracks(tracks) {
		return Api.delete('track', {
			trackIds: tracks.map(track => track.id)
		}).then(() => {
			// Call sortTracks() with no arguments, which will reload the songs for our view (and flush out the deleted ones)
			this.sortTracks();
		})
	}

	loadPlaylists() {
		return Api.get('playlist').then((playlists) => {
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
		return Api.post("playlist/track", {
			playlistId: playlistId,
			trackIds: trackIds
		}).then(() => {
			console.log("Wow, Ayrton. Great moves. Keep it up. I'm proud of you.");
		})
	}

	removeFromPlaylist(trackIds, playlistId) {

	}

	updateTrack(track, displayColumnName, newValue) {
		const columnName = this.trackKeyConversions[displayColumnName];

		let params = { trackId: track.id };
		params[columnName] = newValue;

		track[columnName] = newValue;

		return Api.put('track', params).then(() => {
			console.log('Song data updated');
		})
	}

	renamePlaylist(playlist, newName) {
		playlist.name = newName;

		return Api.put(`playlist/${playlist.id}`, { name: newName }).then(() => {
			console.log('Playlist renamed');
		})
	}

	setShuffleSongs(shuffleSongs) {
		this.setState({ shuffleSongs: shuffleSongs });
		LocalStorage.setBoolean('shuffleSongs', shuffleSongs);
	}

	setRepeatSongs(repeatSongs) {
		this.setState({ repeatSongs: repeatSongs });
		LocalStorage.setBoolean('repeatSongs', repeatSongs);
	}

	resetShuffleIndexes(withoutIndex) {
		if (!this.state.shuffleSongs) {
			return;
		}

		const indexes = Util.range(0, this.state.nowPlayingTracks.length);
		if (withoutIndex !== undefined) {
			indexes.splice(withoutIndex, 1);
		}

		this.setState({ songIndexesToShuffle: indexes })
	}

	// Need to add the new tracks to the shuffle selection or they won't get played until the next run through the playlist
	addTrackIndexesToShuffle(startingIndex, numTracksToAdd) {
		if (!this.state.shuffleSongs) {
			return;
		}

		let adjustedIndexes = [];

		// Adjust the indexes of any songs that are 'after' our new songs on the playlist
		this.state.songIndexesToShuffle.forEach(songIndex => {
			if (songIndex > startingIndex) {
				adjustedIndexes.push(songIndex + numTracksToAdd);
			} else {
				adjustedIndexes.push(songIndex);
			}
		});

		for (let i = 0; i < numTracksToAdd; i++) {
			adjustedIndexes.push(startingIndex + i + 1);
		}

		this.setState({ songIndexesToShuffle: adjustedIndexes })
	}

	render() {
		return (
			<MusicContext.Provider value={this.state}>
				{this.props.children}
			</MusicContext.Provider>
		)
	}
}
