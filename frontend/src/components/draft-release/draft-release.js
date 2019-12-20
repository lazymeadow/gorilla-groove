import React from 'react';
import {Api} from "../../api";
import {Modal} from "../modal/modal";
import {toast} from "react-toastify";

export class DraftRelease extends React.Component {
	constructor(props) {
		super(props);

		this.state = { modalOpen: false }
	}

	setModalOpen(isOpen) {
		this.setState({ modalOpen: isOpen })
	}

	submitHistoryForm(event) {
		event.preventDefault();

		const selectEl = document.getElementById('version-history-device-type');

		const deviceType = selectEl.options[selectEl.selectedIndex].value;
		const version = document.getElementById('version-history-version').value;
		const notes = document.getElementById('version-history-notes').value;

		Api.post(`version/history/deviceType/${deviceType}`, {
			version,
			notes
		}).then(() => {
			this.setModalOpen(false);
			toast.success(`Version history created successfully`);
		}).catch((error) => {
			console.error(error);
			toast.error('The creation of a the new version history failed');
		});
	}

	getDisplayedVersion() {
		return __VERSION__.split('-')[0];
	}

	render() {
		return (
			<div onClick={() => this.setModalOpen(true)}>
				Draft Release
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setModalOpen(false)}
				>
					<form id="version-history-modal" className="form-modal" onSubmit={e => this.submitHistoryForm(e)}>
						<h2>Draft Release</h2>

						<div className="flex-label">
							<label htmlFor="version-history-device-type">Device Type</label>
							<select id="version-history-device-type">
								<option value="WEB">Web</option>
								<option value="ANDROID">Android</option>
							</select>
						</div>

						<div className="flex-label">
							<label htmlFor="version-history-version">Version</label>
							<input
								id="version-history-version"
								name="version-history-version"
								className="medium-property"
								type="text"
								defaultValue={this.getDisplayedVersion()}
								required/>
						</div>

						<label htmlFor="version-history-notes">Version Notes</label>
						<div>
							<textarea id="version-history-notes"/>
						</div>

						<button>Submit</button>
					</form>
				</Modal>
			</div>
		)
	}
}
