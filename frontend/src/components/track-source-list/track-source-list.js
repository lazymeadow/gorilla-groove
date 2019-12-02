import React from 'react';
import TreeView from 'react-treeview'
import {MusicContext} from "../../services/music-provider";
import {TrackView} from "../../enums/track-view";
import {EditableDiv} from "../editable-div/editable-div";
import {AddPlaylistButton} from "../add-playlist/add-playlist";

export class TrackSourceList extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			collapsedBookkeeping: [false, false, false],
			dataSource: [
				{
					section: TrackView.USER,
					heading: 'User Libraries',
					data: []
				}, {
					section: TrackView.PLAYLIST,
					heading: <span className="playlist-heading">Playlists <AddPlaylistButton/></span>,
					data: []
			}
			],
			editedId: null
		};
	}

	componentDidMount() {
		document.body.addEventListener('click', this.handleEditStop.bind(this));
	}

	componentWillUnmount() {
		document.body.removeEventListener('click', this.handleEditStop.bind(this));
	}

	componentWillReceiveProps(props) {
		const userIndex = this.state.dataSource.findIndex(data => {
			return data.section === TrackView.USER
		});

		const playlistsIndex = this.state.dataSource.findIndex(data => {
			return data.section === TrackView.PLAYLIST
		});

		// Update the data to have the latest user data
		let dataSource = this.state.dataSource;
		dataSource[userIndex].data = props.otherUsers;
		dataSource[playlistsIndex].data = props.playlists;

		this.setState({ dataSource });
	}

	handleEditStop(event) {
		if (
			event.target.tagName !== 'INPUT'
			&& event.target.id !== this.state.editedId
			&& this.state.editedId !== null
		) {
			this.setState({ editedId: null })
		}
	}

	handleParentNodeClick(i) {
		let [...collapsedBookkeeping] = this.state.collapsedBookkeeping;
		collapsedBookkeeping[i] = !collapsedBookkeeping[i];
		this.setState({collapsedBookkeeping: collapsedBookkeeping});
	}

	loadOwnLibrary() {
		this.context.loadSongsForUser();
	}

	selectEntry(section, entry, elementId) {
		if (section === this.context.trackView && entry.id === this.context.viewedEntityId) {
			this.setState({ editedId: elementId });
			return; // User selected the same thing as before
		}
		this.setState({ editedId: null });

		if (section === TrackView.USER) {
			this.context.loadSongsForUser(entry.id, false);
		} else {
			this.context.loadSongsForPlaylist(entry.id, false);
		}
	}

	getNowPlayingElement(entry) {
		const song = this.context.nowListeningUsers[entry.email];
		if (!song || !song.trackId) {
			return <span/>
		}

		const artist = song.trackArtist ? song.trackArtist : 'Unknown';
		const name = song.trackName ? song.trackName : 'Unknown';

		return <span className="user-listening" title={`${artist} - ${name}`}>♬</span>
	}

	render() {
		const librarySelected = this.context.trackView === TrackView.LIBRARY ? 'selected' : '';
		return (
			<div id="view-source-list">
				<div
					className={`library-option ${librarySelected}`}
					onClick={() => this.loadOwnLibrary()}
				>
					<span className="my-library">My Library</span>
				</div>
				{this.state.dataSource.map((node, i) => {
					const label =
						<div className="tree-node" onClick={() => this.handleParentNodeClick(i)}>
							{node.heading}
						</div>;
					return (
						<TreeView
							key={i}
							nodeLabel={label}
							collapsed={this.state.collapsedBookkeeping[i]}
							onClick={() => this.handleParentNodeClick(i)}
						>
							{node.data.map(entry => {
								const entrySelected = this.context.trackView === node.section & entry.id === this.context.viewedEntityId;
								const entryClass = entrySelected ? 'selected' : '';
								const cellId = i + '-' + entry.id;
								return (
									<div
										id={cellId}
										className={`tree-child ${entryClass}`}
										key={entry.id}
										onClick={() => this.selectEntry(node.section, entry, cellId)}
									>
										<EditableDiv
											editable={this.state.editedId === cellId && node.section === TrackView.PLAYLIST}
											text={entry.username ? entry.username : entry.name}
											stopEdit={() => this.setState({ editedId: null })}
											updateHandler={newValue => {
												this.context.renamePlaylist(entry, newValue);
												this.forceUpdate();
											}}
										/>
										{ this.getNowPlayingElement(entry) }
									</div>
								)
							})}
						</TreeView>
					);
				})}
			</div>
		);
	}
}
TrackSourceList.contextType = MusicContext;
