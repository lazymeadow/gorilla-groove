import React from 'react';
import {Api} from "..";
import {Modal} from "../modal/modal";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";


export class VersionDisplay extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			modalOpen: false,
			loadingHistory: false,
			historyRecords: []
		}
	}

	componentDidUpdate(prevProps, prevState) {
		if (!prevState.modalOpen && this.state.modalOpen) {
			this.loadHistory();
		}
	}

	loadHistory() {
		this.setState({ loadingHistory: true });

		const params = { limit: 10 };

		return Api.get('version/history/deviceType/WEB', params)
			.then(result => {
				this.setState({ historyRecords: result });
				console.log(result);
			}).catch(error => {
				console.error(error);
			}).finally(() => {
				this.setState({ loadingHistory: false });
			});
	}

	render() {
		return (
			<div id="version-display">
				<div onClick={() => this.setState({ modalOpen: true })} className="version-text">
					{ __VERSION__ }
				</div>
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setState({ modalOpen: false })}
				>
					<div id="changelog">
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
