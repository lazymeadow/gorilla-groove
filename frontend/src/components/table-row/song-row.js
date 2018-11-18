import React from "react";
import {formatDateFromUnixTime, formatTimeFromSeconds} from "../../formatters";

export class SongRow extends React.Component {
	constructor(props) {
		super(props);
	}

	shouldComponentUpdate(nextProps) {
		// We don't want React to re-render every row whenever any row is selected
		// This might have to be tweaked later to look at more properties
		return this.props.selected !== nextProps.selected
			|| this.props.played !== nextProps.played
	}

	// noinspection JSMethodCanBeStatic
	getUserTrackPropertyValue(property, userTrack, rowIndex) {
		switch (property) {
			case '#':
				return rowIndex;
			case 'Name':
				return userTrack.track.name;
			case 'Artist':
				return userTrack.track.artist;
			case 'Album':
				return userTrack.track.album;
			case 'Length':
				return formatTimeFromSeconds(userTrack.track.length);
			case 'Year':
				return userTrack.track.releaseYear;
			case 'Play Count':
				return userTrack.playCount;
			case 'Bit Rate':
				return userTrack.track.bitRate;
			case 'Sample Rate':
				return userTrack.track.sampleRate;
			case 'Added':
				return formatDateFromUnixTime(userTrack.createdAt);
			case 'Last Played':
				return formatDateFromUnixTime(userTrack.lastPlayed);
		}
	}

	render() {
		let selected = this.props.selected ? "selected" : "";
		let played = this.props.played ? "played" : "";
		return (
			<tr
				onClick={(event) => {this.props.onClick(event, this.props.rowIndex)}}
				className={`song-row ${selected} ${played}`}
			>
				{this.props.columns.map((columnName) => {
					return (
						<td>
							<div>
								{this.getUserTrackPropertyValue(columnName, this.props.userTrack, this.props.rowIndex)}
							</div>
						</td>
					)
				})}
			</tr>
		);
	}
}

SongRow.defaultProps = {
	selected: false
};

