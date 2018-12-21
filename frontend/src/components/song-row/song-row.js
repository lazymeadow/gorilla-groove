import React from "react";
import {formatDate, formatTimeFromSeconds} from "../../formatters";
import {MusicContext} from "../../services/music-provider";
import {SongPopoutMenu} from "../popout-menu/song-popout-menu/song-popout-menu";

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
				return userTrack.name;
			case 'Artist':
				return userTrack.artist;
			case 'Album':
				return userTrack.album;
			case 'Length':
				return formatTimeFromSeconds(userTrack.length);
			case 'Year':
				return userTrack.releaseYear;
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
					return (
						<td key={index}>
							<div className={this.props.showContextMenu && index === 0 ? 'shifted-entry' : ''}>
								{this.getUserTrackPropertyValue(columnName, this.props.userTrack, this.props.rowIndex)}
							</div>
						</td>
					)
				})}
				{this.props.showContextMenu ? <td>
					<SongPopoutMenu
						getSelectedTracks={this.props.getSelectedTracks}
					/>
				</td> : <td/>}
			</tr>
		);
	}
}

SongRow.defaultProps = {
	selected: false
};
SongRow.contextType = MusicContext;

