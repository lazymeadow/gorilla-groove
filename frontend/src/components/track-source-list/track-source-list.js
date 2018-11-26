import React from 'react';
import TreeView from 'react-treeview'
import {MusicContext} from "../../services/music-provider";

export class TrackSourceList extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			collapsedBookkeeping: [false, false, false],
			selectedSourceType: 'Library',
			selectedId: 0,
			dataSource: [
				{section: 'Users', data: props.users},
				{section: 'Playlists', data: [{id: 1337, username: 'Playlist'}]}
			]
		}
	}

	componentWillReceiveProps(props) {
		let userIndex = this.state.dataSource.findIndex(data => {
			return data.section === 'Users'
		});

		// Update the data to have the latest user data
		let dataSource = this.state.dataSource;
		dataSource[userIndex].data = props.users;

		this.setState({ dataSource: dataSource });
	}

	handleParentNodeClick(i) {
		let [...collapsedBookkeeping] = this.state.collapsedBookkeeping;
		collapsedBookkeeping[i] = !collapsedBookkeeping[i];
		this.setState({collapsedBookkeeping: collapsedBookkeeping});
	}

	selectLibrary() {
		this.setState({ selectedSourceType: 'Library' });
		this.context.loadSongsForUser(null);
	}

	selectEntry(entry) {
		// Will need to check if this is a user or a playlist when there are playlists
		this.setState({
			selectedSourceType: 'User',
			selectedId: entry.id
		});

		this.context.loadSongsForUser(entry.id);
	}

	render() {
		let librarySelected = this.state.selectedSourceType === 'Library' ? 'selected' : '';
		return (
			<div className="view-source-list">
				View Songs From:
				<div
					className={`library-option ${librarySelected}`}
					onClick={() => this.selectLibrary()}
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
								let entrySelected = this.state.selectedSourceType === 'User' & entry.id === this.state.selectedId;
								let entryClass = entrySelected ? 'selected' : '';
								return (
									<div
										className={`tree-child ${entryClass}`}
										key={entry.id}
										onClick={() => this.selectEntry(entry)}
									>
										{entry.username}
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
