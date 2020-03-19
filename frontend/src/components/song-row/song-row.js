import React from "react";
import {formatDate, formatTimeFromSeconds} from "../../formatters";
import {MusicContext} from "../../services/music-provider";
import {EditableDiv} from "../editable-div/editable-div";
import {TrackView} from "../../enums/site-views";
import {displayKeyToTrackKey} from "../../util";

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
		return this.props.selected !== nextProps.selected;
		return this.props.selected !== nextProps.selected
			|| this.props.played !== nextProps.played
			|| this.props.userTrack.playCount !== nextProps.userTrack.playCount
	}
	*/

	getUserTrackPropertyValue(property, userTrack, rowIndex) {
		switch (property) {
			case '#':
				return rowIndex;
			case 'Length':
				return formatTimeFromSeconds(userTrack.length);
			case 'Added':
				return formatDate(userTrack.createdAt, false);
			case 'Last Played':
				return formatDate(userTrack.lastPlayed, false);
			default:
				return userTrack[displayKeyToTrackKey(property)];
		}
	}

	render() {
		const selected = this.props.selected ? 'selected' : '';
		const playedClass = this.props.played ? 'played' : '';

		return (
			<tr
				onMouseDown={event => { this.props.onClick(event, this.props.rowIndex)}}
				className={`song-row ${selected} ${playedClass}`}
			>
				{this.props.columns.map((columnName, index) => {
					const cellId = `${this.props.rowIndex}-${index}`;
					return (
						<td key={index}>
							<div>
								<EditableDiv
									id={cellId}
									editable={
										SongRow.updatableColumns.has(columnName)
										&& this.props.editableCell === cellId
										&& (this.context.trackView === TrackView.LIBRARY || this.context.trackView === TrackView.PLAYLIST)
									}
									text={this.getUserTrackPropertyValue(columnName, this.props.userTrack, this.props.rowIndex)}
									stopEdit={this.props.stopCellEdits.bind(this)}
									updateHandler={newValue => {
										const newProperties = {};
										newProperties[displayKeyToTrackKey(columnName)] = newValue;

										this.context.updateTracks([this.props.userTrack], null, newProperties);
										this.forceUpdate();
									}}
								/>
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
SongRow.contextType = MusicContext;
SongRow.updatableColumns = new Set(['Album', 'Artist', 'Featuring', 'Name', 'Year', 'Track #', 'Genre', 'Note']);
