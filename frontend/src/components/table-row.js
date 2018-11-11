import React from "react";
import {formatDateFromUnixTime, formatTimeFromSeconds} from "../formatters";

export class TableRow extends React.Component {
	constructor(props) {
		super(props);
	}

	shouldComponentUpdate(nextProps) {
		// We don't want React to re-render every row whenever any row is selected
		// This might have to be tweaked later to look at more properties
		return this.props.selected !== nextProps.selected
	}

	render() {
		return (
			<tr onClick={() => {this.props.onClick(this.props.userTrack)}}>
				<td>{this.props.userTrack.track.name}</td>
				<td>{this.props.userTrack.track.artist}</td>
				<td>{this.props.userTrack.track.album}</td>
				<td>{formatTimeFromSeconds(this.props.userTrack.track.length)}</td>
				<td>{this.props.userTrack.playCount}</td>
				<td>{this.props.userTrack.track.releaseYear}</td>
				<td>{this.props.userTrack.track.bitRate}</td>
				<td>{this.props.userTrack.track.sampleRate}</td>
				<td>{formatDateFromUnixTime(this.props.userTrack.createdAt)}</td>
				<td>{formatDateFromUnixTime(this.props.userTrack.lastPlayed)}</td>
			</tr>
		);
	}
}

TableRow.defaultProps = {
	selected: false
};

