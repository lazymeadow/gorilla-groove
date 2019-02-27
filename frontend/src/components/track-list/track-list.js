import React from 'react';
import ColumnResizer from 'column-resizer';
import * as ReactDOM from "react-dom";
import {SongRow} from "../song-row/song-row";
import {MusicContext} from "../../services/music-provider";
import {SongPopoutMenu} from "../popout-menu/song-popout-menu/song-popout-menu";
import {TrackView} from "../../enums/track-view";

export class TrackList extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			selected: {}, // FIXME Pretty sure I should have made this a set. Pretty sure it's being used like a set
			firstSelectedIndex: null,
			lastSelectedIndex: null,
			withinDoubleClick: false,
			doubleClickTimeout: null,
			editableCell: null,
			pendingEditableCell: null,
			id: this.props.trackView ? 'track-list' : '',
			contextMenuOptions: {
				expanded: false,
				x: 0,
				y: 0
			},
			loading: false
		};
	}

	componentDidMount() {
		this.enableResize();

		// Only the trackView has editable cells. The other view doesn't need to worry about closing them
		if (this.props.trackView) {
			document.body.addEventListener('keydown', e => this.handleKeyPress(e));
			document.body.addEventListener('click', e => this.handleEditStop(e));
			document.getElementsByClassName('border-layout-center')[0]
				.addEventListener('scroll', () => this.handleScroll());
		}
		ReactDOM.findDOMNode(this).addEventListener('contextmenu', e => { this.handleContextMenu(e) });
	}

	componentWillUnmount() {
		this.disableResize();

		if (!this.props.trackView) {
			document.body.removeEventListener('click', e => this.handleEditStop(e));
			document.body.removeEventListener('keydown', e => this.handleKeyPress(e));
			document.getElementsByClassName('border-layout-center')[0]
				.removeEventListener('scroll', () => this.handleScroll());
		}

		ReactDOM.findDOMNode(this).removeEventListener('contextmenu', e => { this.handleContextMenu(e) });
	}

	enableResize() {
		if (!this.props.trackView) {
			return;
		}

		const options = {resizeMode: 'overflow'};
		if (!this.resizer) {
			let tableElement = ReactDOM.findDOMNode(this).querySelector('#' + this.state.id);
			this.resizer = new ColumnResizer(tableElement, options);
		} else {
			this.resizer.reset(options);
		}
	}

	disableResize() {
		if (!this.props.trackView) {
			return;
		}

		if (this.resizer) {
			// TODO reset returns the state of the options, including column widths
			// we could save these off somewhere and use them to re-initialize the table
			this.resizer.reset({ disable: true });
		}
	}

	handleEditStop(event) {
		if (event.target.id !== this.state.editableCell) {
			this.stopCellEdits();
		}
	}

	handleContextMenu(event) {
		if (!this.context.useRightClickMenu) {
			return;
		}

		event.preventDefault();

		let row = event.target.closest('.song-row');
		let rowIndex = [...row.parentElement.children].indexOf(row);

		this.handleRowClick(event, rowIndex);

		this.setState({
			contextMenuOptions: {
				expanded: true,
				trackView: this.props.trackView ? this.context.trackView : TrackView.NOW_PLAYING,
				x: event.clientX - 6,
				y: event.clientY + 8
			}}
		);
	}

	handleKeyPress(event) {
		if (event.key === 'Enter') {
			let indexes = Object.keys(this.state.selected);
			if (indexes.length === 1) {
				this.context.playFromTrackIndex(indexes[0], true);
			} else {
				let tracks = indexes.map(index => this.props.userTracks[index]);
				this.context.playTracks(tracks);
			}
		} else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
			if (this.state.lastSelectedIndex === null) {
				return;
			}

			let newIndex = this.state.lastSelectedIndex + (event.key === 'ArrowDown' ? 1 : -1);

			if (newIndex >= this.props.userTracks.length || newIndex < 0) {
				return;
			}

			let selected = {};
			selected[newIndex] = true;
			this.setState({ selected: selected, lastSelectedIndex: newIndex });

			let trackList = document.getElementById('library-view');
			let selectedRow = trackList.querySelectorAll('.song-row')[newIndex];

			if (event.key === 'ArrowDown') {
				let newScroll = selectedRow.offsetTop - trackList.offsetHeight;
				if (newScroll > trackList.scrollTop) {
					trackList.scrollTop = newScroll + selectedRow.offsetHeight - 10;
				}
			} else {
				if (selectedRow.offsetTop - selectedRow.offsetHeight < trackList.scrollTop) {
					trackList.scrollTop = selectedRow.offsetTop - selectedRow.offsetHeight - 13;
				}
			}
		}
	}

	handleScroll() {
		let scrollBuffer = 500; // Amount of space to scroll to in order to start loading more tracks

		let container = document.getElementsByClassName('border-layout-center')[0];
		if (!this.state.loading
			&& container.scrollHeight < container.offsetHeight + container.scrollTop + scrollBuffer
			&& this.context.viewedTracks.length < this.context.totalTracksToFetch) {

			this.setState({ loading: true });
			this.context.loadMoreTracks().finally(() => {
				this.setState({ loading: false })
			});
		}
	}

	handleRowClick(event, userTrackIndex) {
		let isLeftClick = event.type === 'click';

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
		// Additionally, if right clicking, we don't want to deselect if we selected an already selected row
		if (!event.ctrlKey && !event.shiftKey && !(!isLeftClick && selected[userTrackIndex])) {
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

		if (isLeftClick) {

			// TODO I don't actually make sure you double clicked the SAME row twice. Just that you double clicked. Fix perhaps?
			if (this.state.withinDoubleClick) {
				this.cancelDoubleClick();
				this.context.playFromTrackIndex(userTrackIndex, this.props.trackView);
				this.setState({ editableCell: null })
			} else {
				// Whenever we start a new double click timer, make sure we cancel any old ones lingering about
				this.cancelDoubleClick();
				let pendingEditableCell = this.setupEdit(event, selected, userTrackIndex);
				this.setupDoubleClick(pendingEditableCell);
			}
		}

		this.setState({ selected: selected });
	}

	setupDoubleClick(pendingEditableCell) {
		// Set up a timer that will kill our ability to double click if we are too slow
		let timeout = setTimeout(() => {
			this.setState({ withinDoubleClick: false });
			if (pendingEditableCell) {
				this.setState({
					editableCell: pendingEditableCell,
					pendingEditableCell: null
				})
			}
		}, 300);

		// Set our double clicking state for the next timeout
		this.setState({
			withinDoubleClick: true,
			doubleClickTimeout: timeout
		});
	}

	setupEdit(event, selectedIndexes, userTrackIndex) {
		// If we ALREADY had exactly one thing selected and we clicked the same thing, mark the cell
		// so that it will be edited if the user doesn't click a second time
		if (Object.keys(selectedIndexes).length === 1 && this.state.firstSelectedIndex === userTrackIndex) {

			// If a table cell is empty, it clicks on a different element. So just make sure we grab the ID, either
			// from the current element, or one of its children if the child node was empty and thus not clicked on
			let cellId = null;
			if (event.target.id) {
				cellId = event.target.id;
			} else {
				let elements = event.target.querySelectorAll('[id]');
				cellId = elements ? elements[0].id : null;
			}

			this.setState({ pendingEditableCell: cellId });

			return cellId;
		} else {
			this.setState({
				editableCell: null,
				pendingEditableCell: null
			});

			return null;
		}
	}

	cancelDoubleClick() {
		if (this.state.doubleClickTimeout) {
			window.clearTimeout(this.state.doubleClickTimeout);
			this.setState({
				withinDoubleClick: false,
				doubleClickTimeout: null,
				pendingEditableCell: null
			});
		}
	}

	stopCellEdits() {
		this.setState({ editableCell: null })
	}

	getSelectedTracks() {
		return Object.keys(this.state.selected).map(index => this.props.userTracks[index]);
	}

	getSelectedTrackIndexes() {
		return Object.keys(this.state.selected).map(index => parseInt(index));
	}

	handleHeaderClick(event) {
		// We don't want the 'now playing' view to be able to sort things (at least for now). It gets messy
		if (!this.props.trackView) {
			return;
		}

		let sortColumn = event.currentTarget.querySelector('.column-name').innerHTML.trim();
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

		this.context.reloadTracks(sortColumn, sortDir)
	}

	handleContextMenuOpen(event) {
		event.stopPropagation();
		this.setState({
			contextMenuOptions: {
				expanded: true,
				x: event.clientX - 6,
				y: event.clientY + 8
			}
		})
	}

	closeContextMenu() {
		this.setState({ contextMenuOptions: { expanded: false }})
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
		// noinspection HtmlUnknownTarget
		return (
			<div className="track-list">
				<div>
					<SongPopoutMenu
						context={this.context} // Pass in as prop, so it can be accessed in getDerivedState
						closeContextMenu={() => this.closeContextMenu()}
						getSelectedTracks={() => this.getSelectedTracks()}
						getSelectedTrackIndexes={() => this.getSelectedTrackIndexes()}
						expanded={this.state.contextMenuOptions.expanded}
						trackView={this.state.contextMenuOptions.trackView}
						x={this.state.contextMenuOptions.x}
						y={this.state.contextMenuOptions.y}
					/>
				</div>
				<table id={this.state.id} className="track-table">
					<thead>
					<tr>
						{this.props.columns.map((columnName, index) => {
							return <th key={index} onClick={(e) => this.handleHeaderClick(e)}>
								<div>
									<span className="column-name">{columnName}</span>
									<span className="sort-direction">{this.getSortIndicator(columnName)}</span>
								</div>
							</th>
						})}
					</tr>
					</thead>
					<tbody>
					{this.props.userTracks.map((userTrack, index) => {
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
								showContextMenu={index === this.state.lastSelectedIndex && !this.context.useRightClickMenu}
								openContextMenu={this.handleContextMenuOpen.bind(this)}
								onClick={this.handleRowClick.bind(this)}
								stopCellEdits={this.stopCellEdits.bind(this)}
							/>
						);
					})}
					</tbody>
				</table>
				{
					this.context.loadingTracks && this.props.trackView && this.props.userTracks.length === 0 ? (
						<div className="loading-container">
							<img className="animation-spin" src="./images/logo.png" width="150" height="150"/>
						</div>
					) : <div/>
				}
			</div>
		)
	};
}
TrackList.contextType = MusicContext;
