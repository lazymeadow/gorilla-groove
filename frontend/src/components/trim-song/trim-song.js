import React from 'react';
import {MusicContext} from "../../services/music-provider";
import {Modal} from "../modal/modal";
import {toast} from "react-toastify";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";

export class TrimSong extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			loading: false,
			startTime: '',
			duration: '',
			modalOpen: false
		};
	}

	componentDidMount() {
		this.setState({ startTime: '', duration: '' })
	}

	setModalOpen(isOpen) {
		this.setState({ modalOpen: isOpen })
	}

	inputChange(stateName, event) {
		let newState = {};
		newState[stateName] = event.target.value;

		this.setState(newState);
	}

	// noinspection JSMethodCanBeStatic
	formatTime(userInput) {
		if (userInput.trim().length === 0) {
			return '';
		}

		const errorMessage = 'Time must be in the format "0:00" or "0:00.000"';
		const validationRegex = /^[0-9]{1,2}:[0-9]{2}(\.[0-9]{1,3})?$/;

		if (!validationRegex.test(userInput)) {
			toast.info(errorMessage);
			return null;
		}

		let newInput = userInput;
		const inputPartsPeriod = userInput.split('.');
		const inputPartsColon = userInput.split(':');

		if (inputPartsPeriod.length === 1) {
			newInput += '.000';
		}

		if (inputPartsColon[0].length === 1) {
			newInput = '0' + newInput;
		}

		// The end result will always be in the format '00:00:00.000'
		return '00:' + newInput
	}

	// noinspection JSMethodCanBeStatic
	trimTrack(event) {
		event.preventDefault();

		// This can only be called with a single track selected
		let track = this.props.getSelectedTracks()[0];

		let startTime = this.formatTime(this.state.startTime);
		let duration = this.formatTime(this.state.duration);

		if (startTime === null || duration === null) {
			return;
		}

		if (startTime.length === 0 && duration.length === 0) {
			toast.info('Either a start time or an end time must be specified');
			return;
		}

		this.setState({ loading: true });

		this.context.trimTrack(track, startTime, duration).then(() => {
			this.setState({ modalOpen: false, startTime: '', duration: '' });
			this.context.forceTrackUpdate();
			toast.success('Track trimmed successfully');
		}).catch(() => {
			toast.error('Trim failed. Report this problem')
		}).finally(() => this.setState({ loading: false }));
	}

	render() {
		// noinspection HtmlUnknownTarget
		return (
			<div onClick={() => this.setModalOpen(true)}>
				Trim Song
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setModalOpen(false)}
				>
					<div id="trim-song" className="form-modal p-relative">
						<LoadingSpinner visible={this.state.loading}/>
						<form onSubmit={(e) => this.trimTrack(e)}>

							<h2>Trim Song</h2>

							<hr/>
							<div className="explanation-text">
								<span>
									Minutes and seconds are required (e.g. 2:23).
									<br/>
									Milliseconds can be specified (e.g. 2:23.125)
									<br/>
									Only one box needs to be filled out (but both can be)
								</span>
								<hr/>
							</div>

							<div className="flex-label">
								<label htmlFor="start-time">Start Time</label>
								<input
									id="start-time"
									name="start-time"
									className="long-property"
									placeholder="0:20"
									type="text"
									onChange={(e) => this.inputChange('startTime', e)}
									value={this.state.startTime}
								/>
							</div>

							<div className="flex-label">
								<label htmlFor="duration">Duration</label>
								<input
									id="duration"
									name="duration"
									className="long-property"
									placeholder="5:02.500"
									type="text"
									onChange={(e) => this.inputChange('duration', e)}
									value={this.state.duration}
								/>
							</div>

							<div>
								<button type="submit">Save</button>
							</div>

						</form>
					</div>

				</Modal>
			</div>
		)
	}
}
TrimSong.contextType = MusicContext;
