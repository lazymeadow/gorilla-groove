import React from "react";
import {Api} from "../api";
import {TrackView} from "../enums/TrackView";
import * as LocalStorage from "../local-storage";
import * as Util from "../util";
import {toast} from "react-toastify";
import {findSpotInSortedArray} from "../util";

export const MusicContext = React.createContext();

export class MusicProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			viewedTracks: [],
			trackView: TrackView.LIBRARY,
			viewedEntityId: null, // An ID for the user or library being viewed, or null if viewing the user's own library
			totalTracksToFetch: 0,
			trackSortColumn: 'Artist',
			trackSortDir: 'asc',
			nowPlayingTracks: [],
			playedTrack: null,
			playedTrackIndex: null,
			playlists: [],
			songIndexesToShuffle: [],
			shuffledSongIndexes: [],
			shuffleSongs: LocalStorage.getBoolean('shuffleSongs', false),
			repeatSongs: LocalStorage.getBoolean('repeatSongs', false),
			sessionPlayCounter: 0, // This determines when to "refresh" our now playing song, because you can play an identical song back to back and it's difficult to detect a song change otherwise
			loadSongsForUser: (...args) => this.loadSongsForUser(...args),
			loadMoreTracks: (...args) => this.loadMoreTracks(...args),
			sortTracks: (...args) => this.sortTracks(...args),
			addUploadToExistingLibraryView: (...args) => this.addUploadToExistingLibraryView(...args),
			forceTrackUpdate: (...args) => this.forceTrackUpdate(...args),
			playFromTrackIndex: (...args) => this.playFromTrackIndex(...args),
			playTracks: (...args) => this.playTracks(...args),
			playTracksNext: (...args) => this.playTracksNext(...args),
			playTracksLast: (...args) => this.playTracksLast(...args),
			playIndex: (...args) => this.playIndex(...args),
			playNext: (...args) => this.playNext(...args),
			playPrevious: (...args) => this.playPrevious(...args),
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
			'Genre' : 'genre',
			'Play Count': 'playCount',
			'Bit Rate': 'bitRate',
			'Sample Rate': 'sampleRate',
			'Added': 'createdAt',
			'Last Played': 'lastPlayed',
			'Track #' : 'trackNumber',
			'Note' : 'note'
		};

		this.columnSortKeys = {
			'Name': [{ key: 'name' }],
			'Artist': [{ key: 'artist' }, { key: 'album', dir: 'asc' }, { key: 'trackNumber', dir: 'asc' }],
			'Album': [{ key: 'album' }, { key: 'trackNumber', dir: 'asc' }],
			'Length': [{ key: 'length' }],
			'Year': [{ key: 'releaseYear' }],
			'Play Count': [{ key: 'playCount' }],
			'Genre': [{ key: 'genre' }],
			'Bit Rate': [{ key: 'bitRate' }],
			'Sample Rate': [{ key: 'sampleRate' }],
			'Added': [{ key: 'createdAt' }],
			'Last Played': [{ key: 'lastPlayed' }],
			'Track #' : [{ key: 'trackNumber' }],
			'Note' : [{ key: 'note' }]
		};

		this.pageSize = 75;
	}

	loadSongsForUser(userId, params, append) {
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
			params.sort = this.buildTrackSortParameter(this.state.trackSortColumn, this.state.trackSortDir);
		}
		if (!params.size) {
			params.size = this.pageSize;
		}
		if (!params.page) {
			params.page = 0;
		}

		return Api.get('track', params).then(result => {
			this.addTracksToView(result, append);
		}).catch((error) => {
			console.error(error)
		});
	}

	loadMoreTracks() {
		let page = parseInt(this.state.viewedTracks.length / this.pageSize);

		if (this.state.trackView === TrackView.USER || this.state.trackView === TrackView.LIBRARY) {
			return this.loadSongsForUser(this.state.viewedEntityId, { page: page }, true);
		} else if (this.state.trackView === TrackView.PLAYLIST) {
			return this.loadSongsForPlaylist(this.state.viewedEntityId, { page: page }, true);
		}
	}

	sortTracks(sortColumn, sortDir) {
		let params = {};

		if (sortColumn && sortDir) {
			params.sort = this.buildTrackSortParameter(sortColumn, sortDir);

			this.setState({
				trackSortColumn: sortColumn,
				trackSortDir: sortDir
			});
		} else {
			params.sort = this.buildTrackSortParameter(this.state.trackSortColumn, this.state.trackSortDir);
		}

		if (this.state.trackView === TrackView.USER || this.state.trackView === TrackView.LIBRARY) {
			this.loadSongsForUser(this.state.viewedEntityId, params, false);
		} else if (this.state.trackView === TrackView.PLAYLIST) {
			this.loadSongsForPlaylist(this.state.viewedEntityId, params, false);
		}
	}

	buildTrackSortParameter(columnName, sortDir) {
		let sort = this.columnSortKeys[columnName].slice(0);

		// The first element in the sorting needs to have the direction applied. The other columns don't. They have their own
		sort[0].dir = sortDir;

		return sort.map(sortObject => sortObject.key + ',' + sortObject.dir);
	}

	addUploadToExistingLibraryView(track) {
		if (this.state.trackView !== TrackView.LIBRARY) {
			return;
		}

		let sort = this.columnSortKeys[this.state.trackSortColumn].slice(0);
		sort[0].dir = this.state.trackSortDir;

		const newTrackIndex = findSpotInSortedArray(track, this.state.viewedTracks, sort);
		this.state.viewedTracks.splice(newTrackIndex, 0, track);

		this.setState({ viewedTracks: this.state.viewedTracks });
	}

	playFromTrackIndex(trackIndex, updateNowPlaying) {
		this.setState({
			playedTrackIndex: trackIndex,
			sessionPlayCounter: this.state.sessionPlayCounter + 1,
		});

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
			playedTrackIndex: startIndex,
			sessionPlayCounter: this.state.sessionPlayCounter + 1
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

			let newShuffleHistory = this.state.shuffledSongIndexes.slice(0);
			newShuffleHistory.push(newTrackIndex);

			this.setState({
				songIndexesToShuffle: newShuffleIndexes,
				shuffledSongIndexes: newShuffleHistory
			});
		}

		this.setState({
			playedTrackIndex: newTrackIndex,
			playedTrack: this.state.nowPlayingTracks[newTrackIndex],
			sessionPlayCounter: this.state.sessionPlayCounter + 1
		})
	}

	playNext() {
		if (this.state.shuffleSongs) {
			// If we're shuffling and have more songs to shuffle through, play a random song
			if (this.state.songIndexesToShuffle.length > 0) {
				this.playIndex(this.getRandomIndex())

				// If we are out of songs to shuffle through, but ARE repeating, reset the shuffle list and pick a random one
			} else if (this.state.repeatSongs) {
				this.resetShuffleIndexes();
				this.playIndex(this.getRandomIndex())
			}
		} else {
			// If we aren't shuffling, and we have more songs, just pick the next one
			if (this.state.playedTrackIndex + 1 < this.state.nowPlayingTracks.length) {
				this.playIndex(this.state.playedTrackIndex + 1);

				// Otherwise, if we have run out of songs, but are repeating, start back over from 0
			} else if (this.state.repeatSongs) {
				this.playIndex(0);
			}
		}
	}

	getRandomIndex() {
		let shuffleIndexes = this.state.songIndexesToShuffle;
		return shuffleIndexes[Math.floor(Math.random() * shuffleIndexes.length)];
	}

	playPrevious() {
		if (this.state.shuffleSongs) {
			let shuffledSongIndexes = this.state.shuffledSongIndexes.slice(0);
			if (shuffledSongIndexes.length === 1) {
				// Someone hit play previous on the first song they played. Just start it over
				this.setState({ sessionPlayCounter: this.state.sessionPlayCounter + 1 });
			} else if (shuffledSongIndexes.length > 1) {
				let currentIndex = shuffledSongIndexes.pop();
				let previousIndex = shuffledSongIndexes.pop();

				let indexesToShuffle = this.state.songIndexesToShuffle.slice(0);
				indexesToShuffle.push(currentIndex);
				indexesToShuffle.push(previousIndex);

				this.setState({
					shuffledSongIndexes: shuffledSongIndexes,
					songIndexesToShuffle: indexesToShuffle
				}, () => this.playIndex(previousIndex));
			}
		} else {
			if (this.state.playedTrackIndex > 0) {
				this.playIndex(this.state.playedTrackIndex - 1);
			} else if (this.state.repeatSongs) {
				this.playIndex(this.state.nowPlayingTracks.length - 1);
			} else {
				// Someone hit play previous on the first song they played. Just start it over
				this.setState({ sessionPlayCounter: this.state.sessionPlayCounter + 1 });
			}
		}
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

	loadSongsForPlaylist(playlistId, params, append) {
		params = params ? params : {};
		params.playlistId = playlistId;


		// Messing around with the JPA sorting setup is more hassle than it is worth
		// For sorting playlists, just append 'track.' in front so the key is correct for playlist tracks
		if ('sort' in params) {
			params.sort = params.sort.map(sortTerm => 'track.' + sortTerm);
		}
		if (!params.size) {
			params.size = this.pageSize;
		}
		if (!params.page) {
			params.page = 0;
		}

		this.setState({
			trackView: TrackView.PLAYLIST,
			viewedEntityId: playlistId
		});

		return Api.get('playlist/track', params).then(result => { this.addTracksToView(result, append); })
	}

	addTracksToView(result, append) {
		this.setState({ totalTracksToFetch: result.totalElements });

		if (append) {
			// IF WE ARE APPENDING
			// Assuming we have 75 as our page size, we could have loaded in 1 page, giving us 75 tracks
			// We could have then uploaded a track, that was automatically added to our library if it was
			// sorted into those first 75 tracks. This would give us 76 tracks. We now fetch the 2nd page
			// of tracks, giving us 75 more. However, the 1st track in this request, will actually be the
			// 75th track from before, as it got bumped up with the newly added track. Thus, we mod our
			// total tracks by the page size here, and drop the appropriate number from the beginning of
			// the result. This will give us 2 pages, 150 tracks, with no duplication

			let tracksToDrop = this.state.viewedTracks.length % this.pageSize;
			result.content.splice(0, tracksToDrop);

			this.setState({ viewedTracks: this.state.viewedTracks.concat(result.content) })
		} else {
			this.setState({ viewedTracks: result.content });
		}
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

		return Api.put('track', params).catch((error) => {
			console.error(error);
			toast.error("Failed to updated song data")
		})
	}

	renamePlaylist(playlist, newName) {
		playlist.name = newName;

		return Api.put(`playlist/${playlist.id}`, { name: newName }).catch((error) => {
			console.error(error);
			toast.error("Failed to updated playlist name")
		})
	}

	setShuffleSongs(shuffleSongs) {
		this.setState({
			shuffleSongs: shuffleSongs,
		}, () => {
			if (shuffleSongs) {
				this.resetShuffleIndexes(this.state.playedTrackIndex);
			}
		});
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

		this.setState({
			songIndexesToShuffle: indexes,
			shuffledSongIndexes: [withoutIndex]
		})
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
