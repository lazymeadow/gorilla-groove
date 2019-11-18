import React from "react";
import {Api} from "../api";
import {TrackView} from "../enums/track-view";
import * as LocalStorage from "../local-storage";
import * as Util from "../util";
import {toast} from "react-toastify";
import {findSpotInSortedArray} from "../util";
import {getCookieValue} from "../cookie";
import {isLoggedIn} from "../util";

export const MusicContext = React.createContext();

export class MusicProvider extends React.Component {
	constructor(props) {
		super(props);

		this.trackKeyConversions = {
			'Name': 'name',
			'Artist': 'artist',
			'Featuring': 'featuring',
			'Album': 'album',
			'Track #' : 'trackNumber',
			'Length': 'length',
			'Year': 'releaseYear',
			'Genre' : 'genre',
			'Play Count': 'playCount',
			'Bit Rate': 'bitRate',
			'Sample Rate': 'sampleRate',
			'Added': 'createdAt',
			'Last Played': 'lastPlayed',
			'Note' : 'note'
		};

		this.columnSortKeys = {
			'Name': [{ key: 'name' }],
			'Artist': [{ key: 'artist' }, { key: 'album', dir: 'asc' }, { key: 'trackNumber', dir: 'asc' }],
			'Album': [{ key: 'album' }, { key: 'trackNumber', dir: 'asc' }],
			'Featuring': [{ key: 'featuring' }],
			'Length': [{ key: 'length' }],
			'Year': [{ key: 'releaseYear' }, { key: 'album', dir: 'asc' }, { key: 'trackNumber', dir: 'asc' }],
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

		this.state = {
			viewedTracks: [],
			loadingTracks: false,
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
			useRightClickMenu: LocalStorage.getBoolean('useRightClickMenu', true),
			columnPreferences: this.loadColumnPreferences(),
			sessionPlayCounter: 0, // This determines when to "refresh" our now playing song, because you can play an identical song back to back and it's difficult to detect a song change otherwise
			searchTerm: '',
			nowListeningUsers: {},
			socket: null,

			loadSongsForUser: (...args) => this.loadSongsForUser(...args),
			loadMoreTracks: (...args) => this.loadMoreTracks(...args),
			reloadTracks: (...args) => this.reloadTracks(...args),
			addUploadToExistingLibraryView: (...args) => this.addUploadToExistingLibraryView(...args),
			forceTrackUpdate: (...args) => this.forceTrackUpdate(...args),
			playFromTrackIndex: (...args) => this.playFromTrackIndex(...args),
			playTracks: (...args) => this.playTracks(...args),
			playTracksNext: (...args) => this.playTracksNext(...args),
			playTracksLast: (...args) => this.playTracksLast(...args),
			playNext: (...args) => this.playNext(...args),
			playPrevious: (...args) => this.playPrevious(...args),
			deleteTracks: (...args) => this.deleteTracks(...args),
			setHidden: (...args) => this.setHidden(...args),
			importTracks: (...args) => this.importTracks(...args),
			loadPlaylists: (...args) => this.loadPlaylists(...args),
			loadSongsForPlaylist: (...args) => this.loadSongsForPlaylist(...args),
			addToPlaylist: (...args) => this.addToPlaylist(...args),
			createPlaylist: (...args) => this.createPlaylist(...args),
			removeFromPlaylist: (...args) => this.removeFromPlaylist(...args),
			removeFromNowPlaying: (...args) => this.removeFromNowPlaying(...args),
			trimTrack: (...args) => this.trimTrack(...args),
			updateTracks: (...args) => this.updateTracks(...args),
			renamePlaylist: (...args) => this.renamePlaylist(...args),
			setRepeatSongs: (...args) => this.setRepeatSongs(...args),
			setShuffleSongs: (...args) => this.setShuffleSongs(...args),
			setColumnPreferences: (...args) => this.setColumnPreferences(...args),
			setUseRightClickMenu: (...args) => this.setUseRightClickMenu(...args),
			setSearchTerm: (...args) => this.setSearchTerm(...args),
			resetColumnPreferences: (...args) => this.resetColumnPreferences(...args),
			resetSessionState: (...args) => this.resetSessionState(...args),

			// Socket
			connectToSocket: (...args) => this.connectToSocket(...args),
			disconnectSocket: (...args) => this.disconnectSocket(...args),
			sendPlayEvent: (...args) => this.sendPlayEvent(...args)
		};
	}

	// A user's column preferences are stored in local storage in an object like
	// [{ name: 'Name', enabled: true }, { name: 'Artist', enabled: false }]
	loadColumnPreferences() {
		// Grab the (potentially) already existing preferences
		const columnOptions = Object.keys(this.trackKeyConversions);

		let columnPreferences = LocalStorage.getObject('columnPreferences');

		// If the preferences already existed, we need to check if any new columns were added
		// since the user last logged in.
		if (columnPreferences) {
			let savedColumns = columnPreferences.map(columnPref => columnPref.name );
			let newColumns = Util.arrayDifference(columnOptions, savedColumns);

			if (newColumns.length > 0) {
				// We have new columns to add. Initialize them and add them to the column preferences
				columnPreferences = columnPreferences.concat(newColumns.map(trackColumnName => {
					return { name: trackColumnName, enabled: true };
				}));
				LocalStorage.setObject('columnPreferences', columnPreferences);
			}

		} else {
			// No pre-existing column preferences were found. Enable them all
			columnPreferences = columnOptions.map(trackColumnName => {
				return { name: trackColumnName, enabled: true };
			})
		}

		return columnPreferences;
	}

	buildTrackLoadParams(params) {
		if (!params.size) {
			params.size = this.pageSize;
		}
		if (!params.page) {
			params.page = 0;
		}

		let searchTerm = this.state.searchTerm.trim();
		if (searchTerm) {
			params.searchTerm = searchTerm;
		}
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

		this.buildTrackLoadParams(params);

		if (!append) {
			this.setState({ viewedTracks: [] });
		}
		this.setState({ loadingTracks: true });

		return Api.get('track', params).then(result => {
			this.addTracksToView(result, append);
		}).catch((error) => {
			console.error(error)
		}).finally(() => this.setState({ loadingTracks: false }));
	}

	loadMoreTracks() {
		let page = parseInt(this.state.viewedTracks.length / this.pageSize);

		if (this.state.trackView === TrackView.USER || this.state.trackView === TrackView.LIBRARY) {
			return this.loadSongsForUser(this.state.viewedEntityId, { page: page }, true);
		} else if (this.state.trackView === TrackView.PLAYLIST) {
			return this.loadSongsForPlaylist(this.state.viewedEntityId, { page: page }, true);
		}
	}

	reloadTracks(sortColumn, sortDir) {
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

	// Returns true if another song could be played. False otherwise
	playNext() {
		if (this.state.shuffleSongs) {
			// If we're shuffling and have more songs to shuffle through, play a random song
			if (this.state.songIndexesToShuffle.length > 0) {
				this.playIndex(this.getRandomIndex());

				return true;
				// If we are out of songs to shuffle through, but ARE repeating, reset the shuffle list and pick a random one
			} else if (this.state.repeatSongs) {
				this.resetShuffleIndexes();
				this.playIndex(this.getRandomIndex());

				return true;
			}
		} else {
			// If we aren't shuffling, and we have more songs, just pick the next one
			if (this.state.playedTrackIndex + 1 < this.state.nowPlayingTracks.length) {
				this.playIndex(this.state.playedTrackIndex + 1);

				return true;
				// Otherwise, if we have run out of songs, but are repeating, start back over from 0
			} else if (this.state.repeatSongs) {
				this.playIndex(0);

				return true;
			}
		}

		return false;
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

	// noinspection JSMethodCanBeStatic
	importTracks(tracks) {
		return Api.post('track/import', {
			trackIds: tracks.map(track => track.id)
		});
	}

	deleteTracks(tracks) {
		return Api.delete('track', {
			trackIds: tracks.map(track => track.id)
		}).then(() => {
			// Call reloadTracks() with no arguments, which will reload the songs for our view (and flush out the deleted ones)
			this.reloadTracks();
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

		this.buildTrackLoadParams(params);

		this.setState({
			trackView: TrackView.PLAYLIST,
			viewedEntityId: playlistId,
			loadingTracks: true
		});

		if (!append) {
			this.setState({ viewedTracks: [] });
		}

		return Api.get('playlist/track', params).then(result => {
			// We need to store the playlistTrackId for later, in case we want to remove an entry from the playlist
			// Add this as extra data to the track data, to make sharing the track-list view easy between playlist
			// views, and library / user views
			result.content = result.content.map(playlistTrack => {
				let trackData = playlistTrack.track;
				trackData.playlistTrackId = playlistTrack.id;
				return trackData
			});
			this.addTracksToView(result, append);
		}).finally(() => this.setState({ loadingTracks: false }));
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
		return Api.post('playlist/track', {
			playlistId: playlistId,
			trackIds: trackIds
		}).then(() => {
			console.log("Wow, Ayrton. Great moves. Keep it up. I'm proud of you.");
		})
	}

	removeFromPlaylist(playlistTrackIds) {
		// It's kind of dumb to assume that the playlist we're deleting from is the one we're looking at
		// It's always true right now. But maybe it won't be one day and this will be problematic
		let playlistId = this.state.viewedEntityId;

		return Api.delete('playlist/track', {
			playlistTrackIds: playlistTrackIds
		}).then(() => {
			// Make sure we're still looking at the same playlist before we force the reload
			if (this.state.trackView === TrackView.PLAYLIST && this.state.viewedEntityId === playlistId) {
				let newViewedTracks = this.state.viewedTracks.slice(0);

				// This is a pretty inefficient way to remove stuff. But it's probably fine... right?
				playlistTrackIds.forEach(playlistTrackId => {
					let trackIndex = newViewedTracks.findIndex(track => track.playlistTrackId === playlistTrackId);
					newViewedTracks.splice(trackIndex, 1);
				});

				this.setState({ viewedTracks: newViewedTracks })
			}
		});
	}

	removeFromNowPlaying(trackIndexes) {
		let trackIdSet = new Set(trackIndexes);
		let newNowPlaying = this.state.nowPlayingTracks.filter((track, index) => !trackIdSet.has(index));

		// Handle changing the currently playing song, if we need to
		if (trackIdSet.has(this.state.playedTrackIndex)) {
			// If a song we removed was playing, just stop playing altogether. Might try to do more stuff later
			this.setState({
				playedTrackIndex: null,
				playedTrack: null,
			});
		} else if (this.state.playedTrackIndex !== null) {
			// If we removed tracks BEFORE our now playing track index (i.e. we are playing the 10th song and
			// we removed the 5th song) then we need to shift the now playing track index up by the number of
			// tracks we removed less than the currently played track index
			let indexesToRemove = 0;
			trackIdSet.forEach( indexRemoved => {
				if (indexRemoved < this.state.playedTrackIndex) {
					indexesToRemove++;
				}
			});
			this.setState({ playedTrackIndex: this.state.playedTrackIndex - indexesToRemove });
		}

		this.setState({ nowPlayingTracks: newNowPlaying });
	}

	createPlaylist() {
		Api.post('playlist', {name: 'New Playlist'})
			.then((playlist) => {
				let playlists = this.state.playlists.slice(0);
				playlists.push(playlist);

				this.setState({ playlists: playlists });

				toast.success('New playlist created')
			})
			.catch(error => {
				console.error(error);
				toast.error('The creation of a new playlist failed');
			});
	}

	updateTracks(tracks, albumArt, trackData, usingDisplayNames) {
		// Convert frontend column names to backend names
		let trackParams;

		if (usingDisplayNames) {
			trackParams = Util.mapKeys(trackData, (key) => {
				return this.trackKeyConversions[key];
			});
		} else {
			trackParams = trackData;
		}

		trackParams.trackIds = tracks.map(track => track.id);

		// Update the local track data to be in sync
		tracks.forEach(track => {
			Object.keys(trackParams).forEach(property => {
				track[property] = trackParams[property];
			});
		});

		let params = { updateTrackJson: JSON.stringify(trackParams) };
		if (albumArt) {
			params.albumArt = albumArt;
		}

		// Use Api.upload here because we might have image data
		return Api.upload('PUT', 'track', params).catch(error => {
			console.error(error);
			toast.error("Failed to updated song data")
		})
	}

	// noinspection JSMethodCanBeStatic
	trimTrack(track, startTime, duration) {
		let params = { trackId: track.id };
		if (startTime.length > 0) {
			params.startTime = startTime;
		}

		if (duration.length > 0) {
			params.duration = duration;
		}

		return Api.post('track/trim', params).then(res => {
			track.length = res.newLength;
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

	setColumnPreferences(columnPreferences) {
		this.setState({
			columnPreferences: columnPreferences,
		});
		LocalStorage.setObject('columnPreferences', columnPreferences);
	}

	setUseRightClickMenu(useMenu) {
		this.setState({
			useRightClickMenu: useMenu,
		});
		LocalStorage.setBoolean('useRightClickMenu', useMenu);
	}

	setSearchTerm(searchTerm, callback) {
		this.setState({ searchTerm: searchTerm }, callback);
	}

	resetColumnPreferences() {
		LocalStorage.deleteKey('columnPreferences');
		let preferences = this.loadColumnPreferences();
		this.setState({ columnPreferences: preferences });
	}

	resetSessionState() {
		this.setState({
			playedTrack: null,
			playedTrackIndex: null,
			searchTerm: ''
		})
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

	connectToSocket() {
		if (!isLoggedIn()) {
			return;
		}

		const socket = new WebSocket(Api.getSocketUri());

		socket.onmessage = res => {
			const data = JSON.parse(res.data);

			const email = data.userEmail;
			delete data.userEmail;

			const newNowListeningUsers = Object.assign({}, this.state.nowListeningUsers);
			newNowListeningUsers[email] = data;

			this.setState({ nowListeningUsers: newNowListeningUsers })
		};
		socket.onclose = () => {
			console.log('WebSocket was closed. Reconnecting');
			this.connectToSocket();
		};

		this.setState({
			socket,
			nowListeningUsers: {}
		});
	}

	disconnectSocket() {
		if (this.state.socket) {
			this.state.socket.close();
		}
	}

	sendPlayEvent(track) {
		if (!this.state.socket) {
			return;
		}

		const payload = {
			userEmail: getCookieValue('loggedInEmail')
		};

		// Had a server-side deserialization error once saying this was missing... Not sure how
		if (!payload.userEmail) {
			console.error('No user email found!? Not sending socket message');
			return;
		}

		if (track) {
			payload.trackId = track.id;
			payload.trackArtist = track.hidden ? 'This track' : track.artist;
			payload.trackName = track.hidden ? 'is private' : track.name;
		}

		console.log('About to send socket data', new Date());
		console.log(payload);
		const readyState = this.state.socket.readyState;

		if (readyState === WebSocket.OPEN) {
			this.state.socket.send(JSON.stringify(payload))
		} else if (readyState === WebSocket.CONNECTING) {
			console.info('Socket was still connecting. Ignoring socket send request');
		} else {
			console.info('Socket is in a state of ' + readyState + '. Creating a new socket and ignoring this send request');
			this.connectToSocket();
		}
	}

	render() {
		return (
			<MusicContext.Provider value={this.state}>
				{this.props.children}
			</MusicContext.Provider>
		)
	}
}
