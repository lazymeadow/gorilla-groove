import React from 'react';
import {MusicContext} from "../../services/music-provider";
import {Modal} from "../modal/modal";
import Reorder from 'react-reorder';

export class Settings extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			modalOpen: false,
		};
	}

	componentDidUpdate() {

	}

	setModalOpen(isOpen) {
		this.setState({ modalOpen: isOpen })
	}

	handleColumnReorder(stuff, startingIndex, endingIndex) {
		let columnPreferences = this.context.columnPreferences.slice(0);
		let movedPreference = columnPreferences.splice(startingIndex, 1)[0];
		columnPreferences.splice(endingIndex, 0, movedPreference);

		this.context.setColumnPreferences(columnPreferences);
	}

	render() {
		return (
			<div onClick={() => this.setModalOpen(true)}>
				Settings
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setModalOpen(false)}
				>
					<h2>Settings</h2>
					<div className="column-reorder-list">
						<h3>Click and drag to reorder</h3>
						<Reorder
							reorderId="column-settings-list"
							component="ul"
							onReorder={this.handleColumnReorder.bind(this)}
						>
							{this.context.columnPreferences.map((columnPreference, index) => {
								return (
									<li key={index}>
										<div className="column-preference">
											{columnPreference.name}
										</div>
									</li>
								)
							})}
						</Reorder>
					</div>
					<button onClick={() => this.context.resetColumnPreferences()}>Reset</button>
				</Modal>
			</div>
		)
	}
}
Settings.contextType = MusicContext;
