import React from 'react';
import ColumnResizer from 'column-resizer';
import * as ReactDOM from "react-dom";
import {SongRow} from "../song-row/song-row";
import {MusicContext} from "../../services/music-provider";

export class LibraryList extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			selected: {},
			firstSelectedIndex: null,
			lastSelectedIndex: null,
			withinDoubleClick: false,
			doubleClickTimeout: null,
			id: 'library-list' + LibraryList.count
		};

		// There's more than one library-list component in view, and the column resizing needs a unique ID in order
		// to attach to the tables. So increment this count and use it in the ID whenever we create a library list
		LibraryList.count++;
	}

	componentDidMount() {
		this.enableResize();
	}

	componentWillUnmount() {
		this.disableResize();
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

	handleRowClick(event, userTrackIndex) {
		let selected = this.state.selected;
		this.setState({ lastSelectedIndex: userTrackIndex });

		// Always set the first selected if there wasn't one
		if (!this.state.firstSelectedIndex) {
			this.setState({ firstSelectedIndex: userTrackIndex })
		}

		// If we aren't holding a modifier, we want to deselect all rows that were selected, and remember the track we picked
		if (!event.ctrlKey && !event.shiftKey) {
			selected = {};
			this.setState({ firstSelectedIndex: userTrackIndex })
		}

		// If we're holding shift, we should select only have selected the rows between this click and the first click
		if (event.shiftKey && this.state.firstSelectedIndex) {
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

		if (this.state.withinDoubleClick) {
			this.cancelDoubleClick();
			this.context.playFromTrackIndex(userTrackIndex, this.props.updateNowPlaying);
		} else {
			this.setupDoubleClick();
		}

		selected[userTrackIndex] = true;
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

	render() {
		return (
			<div>
				<table id={this.state.id} className="track-table">
					<thead>
					<tr>
						{this.props.columns.map((columnName, index) => {
							return <th key={index}>{columnName}</th>
						})}
					</tr>
					</thead>
					<tbody>
					{this.props.userTracks.map((userTrack, index) => {
						return (
							<SongRow
								key={index}
								columns={this.props.columns}
								rowIndex={index}
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
LibraryList.contextType = MusicContext;
LibraryList.count = 0;
