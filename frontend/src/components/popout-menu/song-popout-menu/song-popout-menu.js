import React from 'react';
import {MusicContext} from "../../../services/music-provider";
import {toast} from "react-toastify";
import {TrackView} from "../../../enums/track-view";

export class SongPopoutMenu extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			menuOptions: []
		};
	}

	shouldComponentUpdate(nextProps) {
		return nextProps.expanded !== this.props.expanded
			|| nextProps.x !== this.props.x
			|| nextProps.y !== this.props.y;
	}

	static getDerivedStateFromProps(props) {
		if (!props.expanded) {
			return {};
		}

		let options;
		switch (props.trackView) {
			case TrackView.LIBRARY: {
				options = SongPopoutMenu.getBaseMenuOptions(props)
					.concat(SongPopoutMenu.getOwnLibraryOptions(props))
					.concat(SongPopoutMenu.getPlaylistAdditionOptions(props));
				break;
			}
			case TrackView.PLAYLIST: {
				options = SongPopoutMenu.getBaseMenuOptions(props)
					.concat(SongPopoutMenu.getOwnLibraryOptions(props))
					.concat(SongPopoutMenu.getPlaylistAdditionOptions(props))
					.concat(SongPopoutMenu.getPlaylistSpecificOptions(props));
				break;
			}
			case TrackView.USER: {
				options = SongPopoutMenu.getBaseMenuOptions(props)
					.concat(SongPopoutMenu.getOtherUserOptions(props));
				break;
			}
			case TrackView.NOW_PLAYING: {
				options = SongPopoutMenu.getBaseMenuOptions(props)
					.concat(SongPopoutMenu.getNowPlayingOptions(props))
				break;
			}
			default: {
				options = [];
			}
		}

		return ({ menuOptions: options })
	}

	componentDidMount() {
		document.body.addEventListener('click', this.props.closeContextMenu);
	}

	componentWillUnmount() {
		document.body.removeEventListener('click', this.props.closeContextMenu);
	}

	static getBaseMenuOptions(props) {
		return [
			{
				text: "Play Now", clickHandler: (e) => {
					e.stopPropagation();
					props.context.playTracks(props.getSelectedTracks());
				}
			},
			{
				text: "Play Next", clickHandler: (e) => {
					e.stopPropagation();
					props.context.playTracksNext(props.getSelectedTracks());
				}
			},
			{
				text: "Play Last", clickHandler: (e) => {
					e.stopPropagation();
					props.context.playTracksLast(props.getSelectedTracks());
				}
			}];
	}

	static getOwnLibraryOptions(props) {
		return [
			{
				text: "Make Private", clickHandler: (e) => {
					e.stopPropagation();
					const tracks = props.getSelectedTracks();
					props.context.setHidden(tracks, true).then(() => {
						if (tracks.length === 1) {
							toast.success(`'${tracks[0].name}' was made private`);
						} else {
							toast.success(`${tracks.length} tracks were made private`);
						}
					}).catch((error) => {
						console.error(error);
						toast.error('Failed to make the selected tracks private');
					});
				}
			},
			{
				text: "Make Public", clickHandler: (e) => {
					e.stopPropagation();
					const tracks = props.getSelectedTracks();
					props.context.setHidden(tracks, false).then(() => {
						if (tracks.length === 1) {
							toast.success(`'${tracks[0].name}' was made public`);
						} else {
							toast.success(`${tracks.length} tracks were made public`);
						}
					}).catch((error) => {
						console.error(error);
						toast.error('Failed to make the selected tracks public');
					});
				}
			},
			{
				text: "Delete", clickHandler: (e) => {
					e.stopPropagation();
					const tracks = props.getSelectedTracks();
					props.context.deleteTracks(tracks, false).then(() => {
						if (tracks.length === 1) {
							toast.success(`'${tracks[0].name}' was deleted`);
						} else {
							toast.success(`${tracks.length} tracks were deleted`);
						}
					}).catch((error) => {
						console.error(error);
						toast.error('Failed to delete the selected tracks');
					});
				}
			}
		];
	}

	static getPlaylistSpecificOptions(props) {
		return [
			{
				text: "Remove from Playlist", clickHandler: (e) => {
					e.stopPropagation();
					const tracks = props.getSelectedTracks();
					const playlistTrackIds = tracks.map(track => track.playlistTrackId);

					props.context.removeFromPlaylist(playlistTrackIds).then(() => {
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
	}

	static getOtherUserOptions(props) {
		return [
			{
				text: "Import to Library", clickHandler: (e) => {
					e.stopPropagation();
					const tracks = props.getSelectedTracks();
					props.context.importTracks(tracks).then(() => {
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
	}

	static getNowPlayingOptions(props) {
		return [
			{
				text: "Remove", clickHandler: (e) => {
					e.stopPropagation();
					const trackIndexes = props.getSelectedTrackIndexes();
					props.context.removeFromNowPlaying(trackIndexes);
				}
			}
		];
	}

	static getPlaylistAdditionOptions(props) {
		// TODO I'd rather have these nested in a 'Playlists' context menu instead of being here at the root level
		return props.context.playlists.map(playlist => {
			return {
				text: `Add to Playlist: ${playlist.name}`,
				clickHandler: (e) => {
					e.stopPropagation();
					let tracks = props.getSelectedTracks();
					let trackIds = tracks.map(track => track.id);
					props.context.addToPlaylist(playlist.id, trackIds).then(() => {
						if (tracks.length === 1) {
							toast.success(`'${tracks[0].name}' was added to '${playlist.name}'`)
						} else {
							toast.success(`${tracks.length} tracks were added to '${playlist.name}'`)
						}
					}).catch((error) => {
						console.error(error);
						toast.error(`Failed to add the selected tracks to '${playlist.name}'`)
					});
				}
			}
		});
	}

	render() {
		let expandedClass = this.props.expanded ? '' : 'hidden';
		return (
			<div className={`song-popout-menu popout-menu ${expandedClass}`} style={{left: this.props.x, top: this.props.y}}>
				<ul>
					{this.state.menuOptions.map((menuItem, index) => {
						return <li key={index} onClick={menuItem.clickHandler}>{menuItem.text}</li>
					})}
				</ul>
			</div>
		)
	}
}
SongPopoutMenu.contextType = MusicContext;
