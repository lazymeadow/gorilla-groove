import React from 'react';
import TreeView from 'react-treeview'
import {MusicContext} from "../../services/music-provider";
import {TrackView} from "../../enums/TrackView";

export class TrackSourceList extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			collapsedBookkeeping: [false, false, false],
			dataSource: [
				{section: TrackView.USER, data: []},
				{section: TrackView.PLAYLIST, data: []}
			]
		};
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

	handleParentNodeClick(i) {
		let [...collapsedBookkeeping] = this.state.collapsedBookkeeping;
		collapsedBookkeeping[i] = !collapsedBookkeeping[i];
		this.setState({collapsedBookkeeping: collapsedBookkeeping});
	}

	loadOwnLibrary() {
		this.context.loadSongsForUser();
	}

	selectEntry(section, entry) {
		if (section === TrackView.USER) {
			this.context.loadSongsForUser(entry.id);
		} else {
			this.context.loadSongsForPlaylist(entry.id);
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
								return (
									<div
										className={`tree-child ${entryClass}`}
										key={entry.id}
										onClick={() => this.selectEntry(node.section, entry)}
									>
										{entry.username ? entry.username : entry.name}
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
