import React, {useContext, useEffect, useRef} from 'react';
import {MusicContext} from "../../../services/music-provider";
import {toast} from "react-toastify";
import {TrackView} from "../../../enums/site-views";
import {SongProperties} from "../../song-properties/song-properties";
import {Api} from "../../../api";
import {TrimSong} from "../../trim-song/trim-song";
import MetadataRequest from "../../metadata-request/metadata-request";
import {PlaylistContext} from "../../../services/playlist-provider";
import {copyToClipboard, getScreenHeight} from "../../../util";
import PopoutMenu from "../popout-menu";
import RemotePlay from "../../remote-play/modal/remote-play";
import {RemotePlayType} from "../../remote-play/modal/remote-play-type";
import SongDelete from "./song-delete/song-delete";
import RecommendTo from "../../recommend-to/recommend-to";
import {UserContext} from "../../../services/user-provider";
import {PermissionType} from "../../../enums/permission-type";

let menuOptions = [];
let lastExpanded = false;
let lastX = -1;
let lastY = -1;

export default function SongPopoutMenu(props) {
	const musicContext = useContext(MusicContext);
	const userContext = useContext(UserContext);
	const playlistContext = useContext(PlaylistContext);

	const calculateMenuOptions = () => {
		if (!props.expanded) {
			return menuOptions;
		}

		if (props.x === lastX && props.y === lastY && props.expanded === lastExpanded) {
			return menuOptions;
		}

		switch (props.trackView) {
			case TrackView.LIBRARY: {
				return getBaseMenuOptions(props)
					.concat(getOwnLibraryOptions(props))
					.concat(getPlaylistAdditionOptions(props));
			}
			case TrackView.PLAYLIST: {
				return getBaseMenuOptions(props)
					.concat(getOwnLibraryOptions(props))
					.concat(getPlaylistAdditionOptions(props))
					.concat(getPlaylistSpecificOptions(props));
			}
			case TrackView.USER: {
				return getBaseMenuOptions(props)
					.concat(getOtherUserOptions(props));
			}
			case TrackView.NOW_PLAYING: {
				return getBaseMenuOptions(props)
					.concat(getNowPlayingOptions(props));
			}
			default: {
				return [];
			}
		}
	};

	const getBaseMenuOptions = () => {
		let baseOptions = [
			{
				text: 'Play Now', clickHandler: e => {
					e.stopPropagation();
					musicContext.playTracks(props.getSelectedTracks());
				}
			}, {
				text: 'Play Next', clickHandler: e => {
					e.stopPropagation();
					musicContext.playTracksNext(props.getSelectedTracks());
				}
			}, {
				text: 'Play Last', clickHandler: e => {
					e.stopPropagation();
					musicContext.playTracksLast(props.getSelectedTracks());
				}
			}, {
				component: <PopoutMenu
					mainItem={{ text: 'Remote Play' }}
					menuItems={[
						{
							component: <RemotePlay
								title="Play Now"
								playType={RemotePlayType.PLAY_SET_SONGS}
								getSelectedTracks={props.getSelectedTracks}
							/>
						},
						{
							component: <RemotePlay
								title="Play Next"
								playType={RemotePlayType.ADD_SONGS_NEXT}
								getSelectedTracks={props.getSelectedTracks}
							/>
						},
						{
							component: <RemotePlay
								title="Play Last"
								playType={RemotePlayType.ADD_SONGS_LAST}
								getSelectedTracks={props.getSelectedTracks}
							/>
						},
					]}
					expansionOnHover={true}
				/>
			}
		];

		const selectedTracks = props.getSelectedTracks();

		if (selectedTracks.length === 1) {
			baseOptions = baseOptions.concat([{
				text: 'Get Link', clickHandler: e => {
					e.stopPropagation();
					const trackId = selectedTracks[0].id;

					// Call the API as an authenticated user to generate a link. Then save the frontend link to
					// the song's page in the clipboard which will then be able to access the song, because we
					// forced the link to be generated while we were authenticated

					// We have to do this out of sequence, and copy to clipboard before we call the API. Otherwise, FF
					// will get mad that the clipboard copy didn't happen quick enough after the click event......
					const link = Api.getBaseUrl() + '/track-link/' + trackId;
					copyToClipboard(link);

					Api.get('file/link/' + trackId).then(() => {
						toast.success('Link copied to clipboard');
					});
				}
			}, {
				text: 'Download', clickHandler: e => {
					e.stopPropagation();
					const track = selectedTracks[0];

					Api.download(`file/download/${track.id}`);
				}
			}
			])
		}

		return baseOptions;
	};

	const getOwnLibraryOptions = () => {
		let ownLibraryOptions = [
			{
				component: <PopoutMenu
					mainItem={{text: 'Song Visibility'}}
					menuItems={[
						{
							text: 'Make Private', clickHandler: e => {
								e.stopPropagation();
								const tracks = props.getSelectedTracks();
								musicContext.setPrivate(tracks, true).then(() => {
									if (tracks.length === 1) {
										toast.success(`'${tracks[0].name}' was made private`);
									} else {
										toast.success(`${tracks.length} tracks were made private`);
									}
								}).catch(error => {
									console.error(error);
									toast.error('Failed to make the selected tracks private');
								});
							}
						}, {
							text: 'Make Public', clickHandler: e => {
								e.stopPropagation();
								const tracks = props.getSelectedTracks();
								musicContext.setPrivate(tracks, false).then(() => {
									if (tracks.length === 1) {
										toast.success(`'${tracks[0].name}' was made public`);
									} else {
										toast.success(`${tracks.length} tracks were made public`);
									}
								});
							}
						}, {
							text: 'Hide in Library', clickHandler: e => {
								e.stopPropagation();
								const tracks = props.getSelectedTracks();
								const propertyChange = { hidden: true };
								musicContext.updateTracks(tracks, null, propertyChange).then(() => {
									if (tracks.length === 1) {
										toast.success(`'${tracks[0].name}' was hidden`);
									} else {
										toast.success(`${tracks.length} tracks were hidden`);
									}
									if (!musicContext.showHidden) {
										musicContext.reloadTracks();
									}
								});
							}
						}, {
							text: 'Show in Library', clickHandler: e => {
								e.stopPropagation();
								const tracks = props.getSelectedTracks();
								const propertyChange = {hidden: false};
								musicContext.updateTracks(tracks, null, propertyChange).then(() => {
									if (tracks.length === 1) {
										toast.success(`'${tracks[0].name}' was revealed again`);
									} else {
										toast.success(`${tracks.length} tracks were revealed again`);
									}
								}).catch(error => {
									console.error(error);
									toast.error('Failed to make the selected tracks visible');
								});
							}
						}
					]}
					expansionOnHover={true}
				/>
			}, {
				component: <SongProperties getSelectedTracks={props.getSelectedTracks.bind(this)}/>
			}, {
				component: <MetadataRequest getSelectedTracks={props.getSelectedTracks.bind(this)}/>
			}, {
				component: <RecommendTo getSelectedTracks={props.getSelectedTracks.bind(this)}/>,
				shouldRender: userContext.hasPermission(PermissionType.EXPERIMENTAL)
			}, {
				component: <SongDelete getSelectedTracks={props.getSelectedTracks.bind(this)}/>
			}
		];

		if (props.getSelectedTracks().length === 1) {
			ownLibraryOptions = [{
				component: <TrimSong getSelectedTracks={props.getSelectedTracks.bind(this)}/>
			}].concat(ownLibraryOptions);
		}

		return ownLibraryOptions;
	};

	const getPlaylistSpecificOptions = () => {
		return [
			{
				text: 'Remove from Playlist', clickHandler: e => {
					e.stopPropagation();
					const tracks = props.getSelectedTracks();
					const playlistTrackIds = tracks.map(track => track.playlistTrackId);

					const playlistId = musicContext.viewedEntityId;

					playlistContext.removeFromPlaylist(playlistTrackIds).then(() => {
						// Make sure we're still looking at the same playlist before we force the reload
						if (musicContext.trackView === TrackView.PLAYLIST && musicContext.viewedEntityId === playlistId) {
							const newViewedTracks = musicContext.viewedTracks.slice(0);

							// This is a pretty inefficient way to remove stuff. But it's probably fine... right?
							playlistTrackIds.forEach(playlistTrackId => {
								const trackIndex = newViewedTracks.findIndex(track => track.playlistTrackId === playlistTrackId);
								newViewedTracks.splice(trackIndex, 1);
							});

							musicContext.setProviderState({ viewedTracks: newViewedTracks }, musicContext.forceTrackUpdate);
						}

						if (tracks.length === 1) {
							toast.success(`'${tracks[0].name}' was removed`)
						} else {
							toast.success(`${tracks.length} tracks were removed`)
						}
					}).catch(error => {
						console.error(error);
						toast.error('Failed to remove the selected tracks');
					});
				}
			}
		];
	};

	const getOtherUserOptions = () => {
		return [
			{
				text: 'Import to Library', clickHandler: e => {
					e.stopPropagation();
					const tracks = props.getSelectedTracks();
					musicContext.importTracks(tracks).then(() => {
						if (tracks.length === 1) {
							toast.success(`'${tracks[0].name}' was imported`)
						} else {
							toast.success(`${tracks.length} tracks were imported`)
						}
					}).catch(error => {
						console.error(error);
						toast.error('Failed to import the selected tracks');
					});
				}
			}
		];
	};

	const getNowPlayingOptions = () => {
		return [
			{
				text: 'Remove', clickHandler: e => {
					e.stopPropagation();
					musicContext.removeFromNowPlaying(props.selectionKeys);
				}
			}
		];
	};

	const getPlaylistAdditionOptions = () => {
		const playlistItems = playlistContext.playlists.map(playlist => {
			return {
				text: playlist.name,
				clickHandler: e => {
					e.stopPropagation();
					const tracks = props.getSelectedTracks();
					const trackIds = tracks.map(track => track.id);
					playlistContext.addToPlaylist(playlist.id, trackIds).then(() => {
						if (tracks.length === 1) {
							toast.success(`'${tracks[0].name}' was added to '${playlist.name}'`)
						} else {
							toast.success(`${tracks.length} tracks were added to '${playlist.name}'`)
						}
					}).catch(error => {
						console.error(error);
						toast.error(`Failed to add the selected tracks to '${playlist.name}'`)
					});
				}
			}
		});

		return {
			component: <PopoutMenu
				mainItem={{ text: 'Add to Playlist' }}
				menuItems={playlistItems}
				expansionOnHover={true}
			/>
		}
	};

	menuOptions = calculateMenuOptions();

	useEffect(() => {
		// Put it in a timeout so other click handlers resolve first.
		// If we don't, then our menu will close before the click fires on the element
		const closeFunction = e => {
			setTimeout(() => props.closeContextMenu(e))
		};

		if (props.expanded) {
			document.body.addEventListener('click', closeFunction);
		} else if (!props.expanded) {
			document.body.removeEventListener('click', closeFunction);
		}
	}, [props.expanded]);

	const expandedClass = props.expanded ? '' : 'hidden';

	const menuRef = useRef(null);

	// As it turns out, getting an accurate measurement here kind of sucks because the height
	// isn't known until the child renders, and we don't know when that happens. So just do
	// a hacky and dumb "guess" at the height based off the number of rows
	const approximateMenuHeight = menuOptions.length * 17 + 10;
	const screenHeight = getScreenHeight();

	let adjustedY = props.y === undefined ? 0 : props.y;
	let adjustedX = props.x === undefined ? 0 : props.x;
	if (props.y + approximateMenuHeight > screenHeight) {
		adjustedY = screenHeight - approximateMenuHeight;
	}

	return (
		<div
			className={`song-popout-menu ${expandedClass}`}
			ref={menuRef}
			// Tiny x-offset here to keep the right click from opening the context menu on the popped up menu
			style={{ left: adjustedX + 1, top: adjustedY }}
		>
			<PopoutMenu
				menuItems={menuOptions}
				expansionOverride={props.expanded}
			/>
		</div>
	)
}
