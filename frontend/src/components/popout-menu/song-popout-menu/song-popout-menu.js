import React from 'react';
import {PopoutMenu} from "../popout-menu";
import {MusicContext} from "../../../services/music-provider";

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
						this.context.setHidden(this.props.getSelectedTracks(), true);
					}
				},
				{ text: "Make Public", clickHandler: (e) => {
						e.stopPropagation();
						this.context.setHidden(this.props.getSelectedTracks(), false);
					}
				},
				{ text: "Add To Playlist", clickHandler: (e) => {
						e.stopPropagation();
						this.context.setHidden(this.props.getSelectedTracks(), false);
					}
				},
				{ text: "Delete", clickHandler: () => alert("Settings") }
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
					let trackIds = this.props.getSelectedTracks().map(track => track.id);
					this.context.addToPlaylist(playlist.id, trackIds);
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
