import React from "react";
import {formatDate, formatTimeFromSeconds} from "../../formatters";
import {PopoutMenu} from "../popout-menu/popout-menu";

export class SongRow extends React.Component {
	constructor(props) {
		super(props);
	}

	// This is currently causing issues with track-level updates (like updating play count)
	// Because the value is updated directly on the track, this.props and nextProps already
	// have the same value for the so we can't detect a change properly
	/*
	shouldComponentUpdate(nextProps) {
		return this.props.selected !== nextProps.selected
			|| this.props.played !== nextProps.played
			|| this.props.userTrack.playCount !== nextProps.userTrack.playCount
	}
	*/

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
				return formatDate(userTrack.createdAt);
			case 'Last Played':
				return formatDate(userTrack.lastPlayed);
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
				{this.props.columns.map((columnName, index) => {
					return (
						<td key={index}>
							<div className={this.props.showContextMenu && index === 0 ? 'shifted-entry' : ''}>
								{this.getUserTrackPropertyValue(columnName, this.props.userTrack, this.props.rowIndex)}
							</div>
						</td>
					)
				})}
				{this.props.showContextMenu ? <PopoutMenu
						mainItem={{
							className: "song-menu",
							text: '⚙'
						}}
						menuItems={[
							{ text: "Test", clickHandler: () => alert("Settings") }
						]}
					/> : <td/>}
			</tr>
		);
	}
}

SongRow.defaultProps = {
	selected: false
};

