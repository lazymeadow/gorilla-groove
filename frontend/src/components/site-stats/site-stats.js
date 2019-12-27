import React from 'react';
import {Api} from "..";
import {MusicContext} from "../../services/music-provider";
import {VersionDisplay} from "../version-display/version-display";

let oneWeekAgo = Date.now() - (1000 * 60 * 60 * 24 * 7);

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
		// Intentionally using .then() instead of .finally() for loading because
		// if it doesn't complete, I'd rather just display nothing
		Promise.all([this.loadSongHistory(), this.loadSongCount()]).then(() => {
			this.setState({ loading: false })
		});
	}

	loadSongHistory() {
		let params = {
			startDate: oneWeekAgo,
			endDate: Date.now()
		};

		return Api.get('track-history', params)
			.then(result => {
				let totalTime = result.reduce((totalTime, historyEntry) => {
					return totalTime + historyEntry.trackLength;
				}, 0);

				let hoursListened = totalTime / 3600;

				// If we have more than 10 hours, don't worry about the decimal precision
				let formattedHours = hoursListened > 10 ? hoursListened.toFixed(0) : hoursListened.toFixed(1);

				this.setState({
					timeListened: `${formattedHours} hrs`,
					songsListenedTo: result.length
				});
			}).catch((error) => {
				console.error(error);
			});
	}

	loadSongCount() {
		return Api.get("track/all-count-since-timestamp", { timestamp: oneWeekAgo })
			.then(result => {
				this.setState({ songsAdded: result });
			})
			.catch((error) => {
				console.error(error);
			});
	}

	render() {
		return (
			<div id="site-stats">
				<div>
				{
					this.state.loading ? <div/> : (
						<>
							<div className="header">Weekly Site Stats</div>
							<div className="flex-label">
								<label>Song Listens: </label>
								<span>{this.state.songsListenedTo}</span>
							</div>
							<div className="flex-label">
								<label>Time Spent: </label>
								<span>{this.state.timeListened}</span>
							</div>
							<div className="flex-label">
								<label>Songs Added: </label>
								<span>{this.state.songsAdded}</span>
							</div>
						</>
					)
				}
				</div>
				<div className="version-display">
					<VersionDisplay/>
				</div>
			</div>
		);
	}
}
SiteStats.contextType = MusicContext;
