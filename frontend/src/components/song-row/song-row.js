import React from "react";
import {formatDate, formatTimeFromSeconds} from "../../formatters";
import {MusicContext} from "../../services/music-provider";
import {EditableDiv} from "../editable-div/editable-div";

export class SongRow extends React.Component {
	constructor(props) {
		super(props);
		this.state = { }
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
				return userTrack.name;
			case 'Artist':
				return userTrack.artist;
			case 'Album':
				return userTrack.album;
			case 'Track #':
				return userTrack.trackNumber;
			case 'Length':
				return formatTimeFromSeconds(userTrack.length);
			case 'Year':
				return userTrack.releaseYear;
			case 'Genre':
				return userTrack.genre;
			case 'Play Count':
				return userTrack.playCount;
			case 'Bit Rate':
				return userTrack.bitRate;
			case 'Sample Rate':
				return userTrack.sampleRate;
			case 'Added':
				return formatDate(userTrack.createdAt);
			case 'Last Played':
				return formatDate(userTrack.lastPlayed);
			case 'Note':
				return userTrack.note;
		}
	}


	render() {
		let selected = this.props.selected ? "selected" : "";
		let playedClass = this.props.played ? 'played' : '';

		return (
			<tr
				onClick={(event) => {this.props.onClick(event, this.props.rowIndex)}}
				className={`song-row ${selected} ${playedClass}`}
			>
				{this.props.columns.map((columnName, index) => {
					const cellId = `${this.props.rowIndex}-${index}`;
					return (
						<td key={index}>
							<div className={this.props.showContextMenu && index === 0 ? 'shifted-entry' : ''}>
								<EditableDiv
									id={cellId}
									editable={SongRow.updatableColumns.has(columnName) && this.props.editableCell === cellId}
									text={this.getUserTrackPropertyValue(columnName, this.props.userTrack, this.props.rowIndex)}
									stopEdit={this.props.stopCellEdits.bind(this)}
									updateHandler={(newValue) => {
										this.context.updateTrack(this.props.userTrack, columnName, newValue);
										this.forceUpdate();
									}}
								/>
							</div>
						</td>
					)
				})}
				{this.props.showContextMenu ? <td onClick={this.props.openContextMenu} className="song-menu">⚙</td> : <td/>}
			</tr>
		);
	}
}

SongRow.defaultProps = {
	selected: false
};
SongRow.contextType = MusicContext;
SongRow.updatableColumns = new Set(['Album', 'Artist', 'Name', 'Year', 'Track #', 'Genre', 'Note']);
