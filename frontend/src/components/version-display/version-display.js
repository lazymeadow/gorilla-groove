import React from 'react';
import {Api} from "..";
import {Modal} from "../modal/modal";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";


let updateIntervalId = null;

export class VersionDisplay extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			modalOpen: false,
			loadingHistory: false,
			historyRecords: [],
			outOfDate: false
		}
	}

	componentDidMount() {
		const thirtyMinutes = 30 * 60 * 1000;
		updateIntervalId = setInterval(this.checkForNewerVersion.bind(this), thirtyMinutes);
		this.checkForNewerVersion();
	}

	componentWillUnmount() {
		if (updateIntervalId !== null) {
			clearInterval(updateIntervalId);
		}
	}

	isVersionOutOfDate(newVersion) {
		// The versions look like x.x.x-1234abc or w/e but if only the git hash changes then
		// let's not say that someone's stuff is out of date. If we want someone to update
		// we should at least be bumping the minor version in almost all cases.

		let newVersionDotIndex = newVersion.lastIndexOf('-');
		let newVersionWithoutHash = newVersion.substring(0, newVersionDotIndex);

		let oldVersionDotIndex = __VERSION__.lastIndexOf('-');
		let oldVersionWithoutHash = __VERSION__.substring(0, oldVersionDotIndex);

		return newVersionWithoutHash !== oldVersionWithoutHash
	}

	checkForNewerVersion() {
		Api.get('version').then(serverVersion => {
			if (this.isVersionOutOfDate(serverVersion.version)) {
				// Once we've marked it as out of date, there is no reason to continue checking
				clearInterval(updateIntervalId);
				updateIntervalId = null;

				this.setState({
					outOfDate: true
				});
			}
		});
	}

	componentDidUpdate(prevProps, prevState) {
		if (!prevState.modalOpen && this.state.modalOpen) {
			this.loadHistory();
		} else if (prevState.modalOpen && !this.state.modalOpen) {
			this.setState({ historyRecords: [] });
		}
	}

	loadHistory() {
		this.setState({ loadingHistory: true });

		const params = { limit: 10 };

		return Api.get('version/history/deviceType/WEB', params)
			.then(result => {
				this.setState({ historyRecords: result });
			}).catch(error => {
				console.error(error);
			}).finally(() => {
				this.setState({ loadingHistory: false });
			});
	}

	render() {
		return (
			<div id="version-display">
				<span onClick={() => this.setState({ modalOpen: true })} className="version-text">{__VERSION__}</span>
				{
					this.state.outOfDate
						? <i className="fas fa-exclamation-triangle" title="Your GG is out of date. Refresh your browser. Please?"/>
						: <React.Fragment/>
				}
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setState({ modalOpen: false })}
				>
					<div id="changelog" className="p-relative">
						<LoadingSpinner visible={this.state.loadingHistory}/>
						<h2>Changelog</h2>
						<hr/>
						{
							this.state.historyRecords.map(record => (
								<div key={record.id}>
									<h3>{record.version}</h3>
									<div className="notes">
										{record.notes}
									</div>
								</div>
							))
						}
					</div>
				</Modal>
			</div>
		);
	}
}
