import React from 'react';
import {PopoutMenu} from "../popout-menu";
import {MusicContext} from "../../../services/music-provider";
import {toast} from "react-toastify";

export class SongPopoutMenu extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			contextMenuOptions: [
				{ text: "Play Now", clickHandler: (e) => {
						e.stopPropagation();
						this.context.playTracks(this.props.getSelectedTracks())
					}
				},
				{ text: "Play Next", clickHandler: (e) => {
						e.stopPropagation();
						this.context.playTracksNext(this.props.getSelectedTracks())
					}
				},
				{ text: "Play Last", clickHandler: (e) => {
						e.stopPropagation();
						this.context.playTracksLast(this.props.getSelectedTracks())
					}
				},
				{ text: "Make Private", clickHandler: (e) => {
						e.stopPropagation();
						const tracks = this.props.getSelectedTracks();
						this.context.setHidden(tracks, true).then(() => {
							if (tracks.length === 1) {
								toast.success(`'${tracks[0].name}' was made private`)
							} else {
								toast.success(`${tracks.length} tracks were made private`)
							}
						}).catch((error) => {
							console.error(error);
							toast.error('Failed to make the selected tracks private')
						});
					}
				},
				{ text: "Make Public", clickHandler: (e) => {
						e.stopPropagation();
						const tracks = this.props.getSelectedTracks();
						this.context.setHidden(tracks, false).then(() => {
							if (tracks.length === 1) {
								toast.success(`'${tracks[0].name}' was made public`)
							} else {
								toast.success(`${tracks.length} tracks were made public`)
							}
						}).catch((error) => {
							console.error(error);
							toast.error('Failed to make the selected tracks public')
						});
					}
				},
				{ text: "Delete", clickHandler: (e) => {
						e.stopPropagation();
						const tracks = this.props.getSelectedTracks();
						this.context.deleteTracks(tracks, false).then(() => {
							if (tracks.length === 1) {
								toast.success(`'${tracks[0].name}' was deleted`)
							} else {
								toast.success(`${tracks.length} tracks were deleted`)
							}
						}).catch((error) => {
							console.error(error);
							toast.error('Failed to delete the selected tracks')
						});
					}
				}
			]
		};
	}

	componentDidMount() {
		// TODO I'd rather have these nested in a 'Playlists' context menu instead of being here at the root level
		let playlistOptions = this.context.playlists.map(playlist => {
			return {
				text: `Add to Playlist: ${playlist.name}`,
				clickHandler: (e) => {
					e.stopPropagation();
					let tracks = this.props.getSelectedTracks();
					let trackIds = tracks.map(track => track.id);
					this.context.addToPlaylist(playlist.id, trackIds).then(() => {
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

		this.setState({contextMenuOptions: this.state.contextMenuOptions.concat(playlistOptions)});
	}

	render() {
		return (
			<PopoutMenu
				mainItem={{
					className: "song-menu",
					text: '⚙'
				}}
				menuItems={this.state.contextMenuOptions}
			/>
		)
	}
}
SongPopoutMenu.contextType = MusicContext;
