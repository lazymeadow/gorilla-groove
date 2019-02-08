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

	handleColumnReorder(event, startingIndex, endingIndex) {
		let columnPreferences = this.context.columnPreferences.slice(0);
		let movedPreference = columnPreferences.splice(startingIndex, 1)[0];
		columnPreferences.splice(endingIndex, 0, movedPreference);

		this.context.setColumnPreferences(columnPreferences);
	}

	toggleColumnEnabled(event) {
		let columnIndex = event.currentTarget.getAttribute('data-column-index');
		let columnPreferences = this.context.columnPreferences.slice(0);
		columnPreferences[columnIndex].enabled = !columnPreferences[columnIndex].enabled;

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
					<div className="column-setting-wrapper">
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
						<div>
							<h3>Click to hide or show</h3>
							<ul>
								{this.context.columnPreferences.map((columnPreference, index) => {
									let columnClass = columnPreference.enabled ? 'column-enabled' : 'column-disabled';
									return (
										<li key={index}>
											<div
												onClick={e => this.toggleColumnEnabled(e)}
												className={`column-toggle ${columnClass}`}
												data-column-index={index}
											/>
										</li>
									)
								})}
							</ul>
						</div>
						<div>

							<h3>Override browser context menu</h3>

							<input
								type="radio"
								id="yes-context-good"
								name="rightClickMenu"
								checked={this.context.useRightClickMenu === true}
								onChange={() => this.context.setUseRightClickMenu(true)}
							/>
							<label htmlFor="yes-context-good">Yes please</label>

							<input
								type="radio"
								id="no-context-bad"
								name="rightClickMenu"
								checked={this.context.useRightClickMenu === false}
								onChange={() => this.context.setUseRightClickMenu(false)}
							/>
							<label htmlFor="no-context-bad">No thanks</label>

						</div>
					</div>
					<button onClick={() => this.context.resetColumnPreferences()}>Reset</button>
				</Modal>
			</div>
		)
	}
}
Settings.contextType = MusicContext;
