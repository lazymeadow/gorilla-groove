import React from 'react';
import ColumnResizer from 'column-resizer';
import * as ReactDOM from "react-dom";
import {TableRow} from "../table-row";

export class LibraryList extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			selected: {}
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

	handleRowClick(userTrack) {
		let selected = this.state.selected;
		selected[userTrack.id] = !selected[userTrack.id];
		this.setState({selected: selected});
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
