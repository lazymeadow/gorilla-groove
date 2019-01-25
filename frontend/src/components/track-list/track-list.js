import React from 'react';
import ColumnResizer from 'column-resizer';
import * as ReactDOM from "react-dom";
import {SongRow} from "../song-row/song-row";
import {MusicContext} from "../../services/music-provider";

export class TrackList extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			selected: {},
			firstSelectedIndex: null,
			lastSelectedIndex: null,
			withinDoubleClick: false,
			doubleClickTimeout: null,
			editableCell: null,
			id: 'track-list' + TrackList.count
		};

		// There's more than one track-list component in view, and the column resizing needs a unique ID in order
		// to attach to the tables. So increment this count and use it in the ID whenever we create a track list
		// UPDATE: not sure that this actually worked...
		TrackList.count++;
	}

	componentDidMount() {
		this.enableResize();

		// Only the trackView has editable cells. The other view doesn't need to worry about closing them
		if (this.props.trackView) {
			document.body.addEventListener('click', (e) => this.handleEditStop(e));
		}
	}

	componentWillUnmount() {
		this.disableResize();

		if (!this.props.trackView) {
			document.body.removeEventListener('click', (e) => this.handleEditStop(e));
		}

	}

	componentDidUpdate() {
		this.enableResize();
	}

	componentWillUpdate() {
		this.disableResize();
	}

	enableResize() {
		const options = {resizeMode: 'overflow'};
		if (!this.resizer) {
			let tableElement = ReactDOM.findDOMNode(this).querySelector('#' + this.state.id);
			this.resizer = new ColumnResizer(tableElement, options);
		} else {
			this.resizer.reset(options);
		}
	}

	disableResize() {
		if (this.resizer) {
			// TODO reset returns the state of the options, including column widths
			// we could save these off somewhere and use them to re-initialize the table
			this.resizer.reset({ disable: true });
		}
	}

	handleEditStop(event) {
		if (event.target.id !== this.state.editableCell) {
			this.setState({ editableCell: null })
		}
	}

	handleRowClick(event, userTrackIndex) {
		if (event.target.tagName === 'INPUT') {
			return; // If we clicked an input just ignore the click entirely since we were editing a song
		}

		let selected = this.state.selected;
		this.setState({ lastSelectedIndex: userTrackIndex });

		// Always set the first selected if there wasn't one
		if (this.state.firstSelectedIndex === null) {
			this.setState({ firstSelectedIndex: userTrackIndex })
		}

		// If we aren't holding a modifier, we want to deselect all rows that were selected, and remember the track we picked
		if (!event.ctrlKey && !event.shiftKey) {
			selected = {};
			this.setState({ firstSelectedIndex: userTrackIndex })
		}

		// If we're holding shift, we should select the rows between this click and the first click
		if (event.shiftKey && this.state.firstSelectedIndex !== null) {
			selected = {};
			let startingRow = Math.min(this.state.firstSelectedIndex, userTrackIndex);
			let endingRow = Math.max(this.state.firstSelectedIndex, userTrackIndex);
			if (startingRow < endingRow) {
				endingRow++
			}

			for (let i = startingRow; i < endingRow; i++) {
				selected[i] = true;
			}
		}

		// The track we clicked needs to always be selected
		selected[userTrackIndex] = true;

		// TODO I don't actually make sure you double clicked the SAME row twice. Just that you double clicked. Fix perhaps?
		if (this.state.withinDoubleClick) {
			this.cancelDoubleClick();
			this.context.playFromTrackIndex(userTrackIndex, this.props.trackView);
			this.setState({ editableCell: null })
		} else {
			this.setupDoubleClick();

			// If we ALREADY had exactly one thing selected and we clicked the same thing, edit the cell
			if (Object.keys(selected).length === 1 && this.state.firstSelectedIndex === userTrackIndex) {
				this.setState({ editableCell: event.target.id })
			} else {
				this.setState({ editableCell: null })
			}
		}

		this.setState({ selected: selected });
	}

	setupDoubleClick() {
		// Whenever we start a new double click timer, make sure we cancel any old ones lingering about
		this.cancelDoubleClick();

		// Set up a timer that will kill our ability to double click if we are too slow
		let timeout = setTimeout(() => {
			this.setState({ withinDoubleClick: false });
		}, 300);

		// Set our double clicking state for the next timeout
		this.setState({
			withinDoubleClick: true,
			doubleClickTimeout: timeout
		});
	}

	cancelDoubleClick() {
		if (this.state.doubleClickTimeout) {
			window.clearTimeout(this.state.doubleClickTimeout);
			this.setState({
				withinDoubleClick: false,
				doubleClickTimeout: null
			});
		}
	}

	getSelectedTracks() {
		return Object.keys(this.state.selected).map(index => this.props.userTracks[index]);
	}

	handleHeaderClick(event) {
		// We don't want the 'now playing' view to be able to sort things (at least for now). It gets messy
		if (!this.props.trackView) {
			return;
		}

		let sortColumn = event.target.querySelector('.column-name').innerHTML.trim();
		let sortDir;

		if (this.context.trackSortColumn === sortColumn) {
			sortDir = this.context.trackSortDir === 'desc' ? 'asc' : 'desc';
		} else {
			sortDir = 'asc';
		}

		this.setState({
			selected: {},
			lastSelectedIndex: null
		});

		this.context.sortTracks(sortColumn, sortDir)
	}

	getSortIndicator(columnName) {
		if (!this.props.trackView) {
			return;
		}

		if (columnName === this.context.trackSortColumn) {
			return this.context.trackSortDir === 'asc' ? ' ▲' : ' ▼';
		} else {
			return '';
		}
	}

	render() {
		return (
			<div>
				<table id={this.state.id} className="track-table">
					<thead>
					<tr>
						{this.props.columns.map((columnName, index) => {
							return <th key={index} onClick={(e) => this.handleHeaderClick(e)}>
								<span className="column-name">{columnName}</span>
								<span className="sort-direction">{this.getSortIndicator(columnName)}</span>
							</th>
						})}
					</tr>
					</thead>
					<tbody>
					{this.props.userTracks.map((userTrack, index) => {
						// We determine if a song in the view is the currently playing song differently based off which view we are in
						// Because we can sort the main track view, and there are no duplicates, it makes sense to calculate this based
						// on the ID of the currently playing track. We can't do this with the "now playing" list, as the same track
						// could be on the list multiple times
						// TODO a more elegant solution IS going to be required though. When playlists are implemented, a song could
						// repeat multiple times in the playlist. The frontend might need to assign its own ID independent of the backend
						let played;
						if (this.props.trackView) {
							played = this.context.playedTrack && this.context.playedTrack.id === userTrack.id;
						} else {
							played = this.context.playedTrackIndex === index;
						}
						return (
							<SongRow
								key={index}
								columns={this.props.columns}
								rowIndex={index}
								editableCell={this.state.editableCell}
								played={played}
								userTrack={userTrack}
								selected={this.state.selected[index.toString()]}
								showContextMenu={index === this.state.lastSelectedIndex}
								onClick={this.handleRowClick.bind(this)}
								getSelectedTracks={() => this.getSelectedTracks()}
							/>
						);
					})}
					</tbody>
				</table>
			</div>
		)
	};
}
TrackList.contextType = MusicContext;
TrackList.count = 0;
