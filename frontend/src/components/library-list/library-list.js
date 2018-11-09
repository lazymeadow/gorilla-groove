import React from 'react';
import ColumnResizer from 'column-resizer';
import {formatDateFromUnixTime, formatTimeFromSeconds} from "../../formatters";
import * as ReactDOM from "react-dom";

export class LibraryList extends React.Component {
	constructor(props) {
		super(props);
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
							<tr onClick={() => {this.props.playSong(userTrack)}} key={userTrack.id}>
								<td>{userTrack.track.name}</td>
								<td>{userTrack.track.artist}</td>
								<td>{userTrack.track.album}</td>
								<td>{formatTimeFromSeconds(userTrack.track.length)}</td>
								<td>{userTrack.playCount}</td>
								<td>{userTrack.track.releaseYear}</td>
								<td>{userTrack.track.bitRate}</td>
								<td>{userTrack.track.sampleRate}</td>
								<td>{formatDateFromUnixTime(userTrack.createdAt)}</td>
								<td>{formatDateFromUnixTime(userTrack.lastPlayed)}</td>
							</tr>
						);
					})}
					</tbody>
				</table>
			</div>
		)
	};
}
