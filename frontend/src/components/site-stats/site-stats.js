import React from 'react';
import {Api} from "..";
import {MusicContext} from "../../services/music-provider";
import {formatTimeFromSeconds} from "../../formatters";

export class SiteStats extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			loading: true,
			songsListenedTo: 0,
			timeListened: 0,
			songsAdded: 0
		}
	}

	componentDidMount() {
		this.loadSongHistory();
	}

	loadSongHistory() {
		let params = {
			startDate: Date.now() - 10000000000,
			endDate: Date.now()
		};

		Api.get("track-history", params)
			.then(result => {
				let totalTime = result.reduce((totalTime, historyEntry) => {
					return totalTime + historyEntry.trackLength;
				}, 0);

				let hoursListened = totalTime / 3600;

				this.setState({
					timeListened: `${hoursListened.toFixed(1)} hours`,
					songsListenedTo: result.length
				})
			})
			.catch((error) => {
				console.error(error);
			});
	}

	render() {
		return (
			<div id="site-stats">
				<div className="header">Site Stats</div>
				<div className="flex-label">
					<label>Song Listens: </label>
					<span>{this.state.songsListenedTo}</span>
				</div>
				<div className="flex-label">
					<label>Time Spent: </label>
					<span>{this.state.timeListened}</span>
				</div>
			</div>
		);
	}
}
SiteStats.contextType = MusicContext;
