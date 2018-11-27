import React from "react";
import {formatDate, formatTimeFromSeconds} from "../../formatters";
import {PopoutMenu} from "../popout-menu/popout-menu";
import {MusicContext} from "../../services/music-provider";

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
		let isPlayed = this.context.playedTrackIndex && this.context.playedTrackIndex === this.props.rowIndex;
		let playedClass = isPlayed ? 'played' : '';

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
				{this.props.showContextMenu ? <td><PopoutMenu
						mainItem={{
							className: "song-menu",
							text: '⚙'
						}}
						menuItems={[
							{ text: "Play Now", clickHandler: (e) => {
									e.stopPropagation();
									this.context.playTracks(this.props.getSelectedTracks())
								}
							},
							{ text: "Play Next", clickHandler: (e) => {
									e.stopPropagation();
									this.context.playTracksNext(this.props.getSelectedTracks())
								}
							},
							{ text: "Play Last", clickHandler: (e) => {
									e.stopPropagation();
									this.context.playTracksLast(this.props.getSelectedTracks())
								}
							},
							{ text: "Make Private", clickHandler: (e) => {
									e.stopPropagation();
									this.context.setHidden(this.props.getSelectedTracks(), true);
								}
							},
							{ text: "Make Public", clickHandler: (e) => {
									e.stopPropagation();
									this.context.setHidden(this.props.getSelectedTracks(), false);
								}
							},
							{ text: "Delete", clickHandler: () => alert("Settings") }
						]}
				/></td> : <td/>}
			</tr>
		);
	}
}

SongRow.defaultProps = {
	selected: false
};
SongRow.contextType = MusicContext;

