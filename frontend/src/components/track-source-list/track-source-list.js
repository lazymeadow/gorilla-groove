import React from 'react';
import TreeView from 'react-treeview'
import {MusicContext} from "../../services/music-provider";
import {TrackView} from "../../enums/track-view";
import {EditableDiv} from "../editable-div/editable-div";

export class TrackSourceList extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			collapsedBookkeeping: [false, false, false],
			dataSource: [
				{section: TrackView.USER, data: []},
				{section: TrackView.PLAYLIST, data: []}
			],
			editedId: null
		};
	}

	componentDidMount() {
		document.body.addEventListener('click', (e) => this.handleEditStop(e));
	}

	componentWillUnmount() {
		document.body.removeEventListener('click', (e) => this.handleEditStop(e));
	}

	componentWillReceiveProps(props) {
		let userIndex = this.state.dataSource.findIndex(data => {
			return data.section === TrackView.USER
		});

		let playlistsIndex = this.state.dataSource.findIndex(data => {
			return data.section === TrackView.PLAYLIST
		});

		// Update the data to have the latest user data
		let dataSource = this.state.dataSource;
		dataSource[userIndex].data = props.otherUsers;
		dataSource[playlistsIndex].data = props.playlists;

		this.setState({ dataSource: dataSource });
	}

	handleEditStop(event) {
		if (event.target.tagName !== 'INPUT' && event.target.id !== this.state.editedId) {
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

	render() {
		let librarySelected = this.context.trackView === TrackView.LIBRARY ? 'selected' : '';
		return (
			<div className="view-source-list">
				View Songs From:
				<div
					className={`library-option ${librarySelected}`}
					onClick={() => this.loadOwnLibrary()}
				>
					Library
				</div>
				{this.state.dataSource.map((node, i) => {
					const label =
						<span className="tree-node" onClick={() => this.handleParentNodeClick(i)}>
							{node.section}
						</span>;
					return (
						<TreeView
							key={i}
							nodeLabel={label}
							collapsed={this.state.collapsedBookkeeping[i]}
							onClick={() => this.handleParentNodeClick(i)}
						>
							{node.data.map(entry => {
								let entrySelected = this.context.trackView === node.section & entry.id === this.context.viewedEntityId;
								let entryClass = entrySelected ? 'selected' : '';
								let cellId = i + '-' + entry.id;
								return (
									<div
										id={cellId}
										className={`tree-child ${entryClass}`}
										key={entry.id}
										onClick={() => this.selectEntry(node.section, entry, cellId)}
									>
										<EditableDiv
											editable={this.state.editedId === cellId}
											text={entry.username ? entry.username : entry.name}
											stopEdit={() => this.setState({ editedId: null })}
											updateHandler={(newValue) => {
												this.context.renamePlaylist(entry, newValue);
												this.forceUpdate();
											}}
										/>

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
