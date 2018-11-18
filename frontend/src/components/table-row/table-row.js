import React from "react";
import {formatDateFromUnixTime, formatTimeFromSeconds} from "../../formatters";

export class TableRow extends React.Component {
	constructor(props) {
		super(props);
	}

	shouldComponentUpdate(nextProps) {
		// We don't want React to re-render every row whenever any row is selected
		// This might have to be tweaked later to look at more properties
		return this.props.selected !== nextProps.selected
			|| this.props.played !== nextProps.played
	}

	render() {
		let selected = this.props.selected ? "selected" : "";
		let played = this.props.played ? "played" : "";
		return (
			<tr
				onClick={(event) => {this.props.onClick(event, this.props.rowIndex)}}
				className={`song-row ${selected} ${played}`}
			>
				<td><div>{this.props.userTrack.track.name}</div></td>
				<td><div>{this.props.userTrack.track.artist}</div></td>
				<td><div>{this.props.userTrack.track.album}</div></td>
				<td><div>{formatTimeFromSeconds(this.props.userTrack.track.length)}</div></td>
				<td><div>{this.props.userTrack.playCount}</div></td>
				<td><div>{this.props.userTrack.track.releaseYear}</div></td>
				<td><div>{this.props.userTrack.track.bitRate}</div></td>
				<td><div>{this.props.userTrack.track.sampleRate}</div></td>
				<td><div>{formatDateFromUnixTime(this.props.userTrack.createdAt)}</div></td>
				<td><div>{formatDateFromUnixTime(this.props.userTrack.lastPlayed)}</div></td>
			</tr>
		);
	}
}

TableRow.defaultProps = {
	selected: false
};

