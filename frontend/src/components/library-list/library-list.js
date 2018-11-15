import React from 'react';
import ColumnResizer from 'column-resizer';
import * as ReactDOM from "react-dom";
import {TableRow} from "../table-row/table-row";

export class LibraryList extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			selected: {},
			firstSelected: null
		}
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
			let tableElement = ReactDOM.findDOMNode(this).querySelector('#track-table');
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

	handleRowClick(event, userTrack) {
		console.log(event);
		let selected = this.state.selected;

		// Always set the first selected if there wasn't one
		if (!this.state.firstSelected) {
			this.setState({ firstSelected: userTrack.id })
		}

		// If we aren't holding a modifier, we want to deselect all rows that were selected, and remember the track we picked
		if (!event.ctrlKey && !event.shiftKey) {
			selected = {};
			this.setState({ firstSelected: userTrack.id })
		}

		// If we're holding shift, we should select only have selected the rows between this click and the first click
		if (event.shiftKey && this.state.firstSelected) {
			selected = {};
			let startingRow = Math.min(this.state.firstSelected, userTrack.id);
			let endingRow = Math.max(this.state.firstSelected, userTrack.id);
			if (startingRow < endingRow) {
				endingRow++
			}

			for (let i = startingRow; i < endingRow; i++) {
				selected[i] = true;
			}
		}

		selected[userTrack.id] = true;
		this.setState({ selected: selected });
	}

	render() {
		return (
			<div>
				<table id="track-table" className="track-table">
					<thead>
					<tr>
						<th>Title</th>
						<th>Artist</th>
						<th>Album</th>
						<th>Length</th>
						<th>Plays</th>
						<th>Year</th>
						<th>Bit Rate</th>
						<th>Sample Rate</th>
						<th>Added</th>
						<th>Last Played</th>
					</tr>
					</thead>
					<tbody>
					{this.props.userTracks.map((userTrack) => {
						return (
							<TableRow
								key={userTrack.id}
								userTrack={userTrack}
								selected={this.state.selected[userTrack.id.toString()]}
								onClick={this.handleRowClick.bind(this)}
							/>
						);
					})}
					</tbody>
				</table>
			</div>
		)
	};
}
