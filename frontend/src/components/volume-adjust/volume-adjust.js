import React, {useState} from 'react';
import {Modal} from "../modal/modal";
import {toast} from "react-toastify";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {Api} from "../../api";

function VolumeAdjustModal(props) {
	const [loading, setLoading] = useState(false);
	const [volume, setVolume] = useState(0.5);
	const [percentage, setPercentage] = useState('1');

	const track = props.getSelectedTracks()[0];

	const adjustVolume = event => {
		event.preventDefault();

		const parsedPercentage = parseFloat(percentage);

		if (parsedPercentage === 1.0) {
			props.closeFunction();
			toast.info('Not adjusting volume as no change was made');
			return;
		}

		if (isNaN(parsedPercentage)) {
			toast.info('The percentage is invalid. It should be numeric');
			return;
		}

		if (parsedPercentage <= 0) {
			toast.info('The percentage may not be less than or equal to 0');
			return;
		}

		setLoading(true);

		Api.post(`track/${track.id}/volume-adjust`, { volumeAdjustAmount: percentage }).then(() => {
			props.closeFunction();
			toast.success(`Track volume adjusted successfully`)
		}).catch(error => {
			if (error.message) {
				toast.error(error.message);
			} else {
				console.error(error);
				toast.error('Failed to adjust track volume');
			}
			setLoading(false);
		});
	};

	const onVolumeAdjust = event => {
		const rawVolume = event.target.value;

		// This is the math to convert between them, based off a best fit line that converts the following:
		// 0 -> 0.25
		// 0.25 -> 0.5
		// 0.5 -> 1
		// 0.75 -> 2
		// 1 -> 4
		const newPercentage = 0.25 * Math.pow(16, rawVolume);

		setVolume(rawVolume);
		setPercentage(newPercentage.toFixed(2))
	};

	const onPercentageSet = event => {
		const rawPercentage = parseFloat(event.target.value);
		if (isNaN(rawPercentage)) {
			setPercentage('');
			return;
		}

		const newVolume = 0.5 + 0.3606737602 * Math.log(rawPercentage);

		setPercentage(rawPercentage);
		setVolume(newVolume);
	};

	return (
		<div id="volume-adjust-modal" className="p-relative">
			<LoadingSpinner visible={loading} small={true}/>
			<h2 className="text-center">Adjust Volume</h2>
			<small>Adjust this song's volume as a percentage of its existing volume</small>

			<form onSubmit={adjustVolume}>
				<div className="text-center slider-container">
					<div className="full-width flex-between">
						<span>½</span>
						<span>1</span>
						<span>2</span>
					</div>
					<input
						type="range"
						onChange={onVolumeAdjust}
						className="volume-range d-block"
						value={volume}
						min="0.25"
						max="0.75"
						step="0.004"
					/>
					<input
						onChange={onPercentageSet}
						className="d-block percentage-input"
						type="number"
						step="0.01"
						value={percentage}
					/>
				</div>

				<div className="flex-between confirm-modal-buttons">
					<button type="submit">
						Pump up the volume
					</button>
					<button type="button" onClick={e => { e.stopPropagation(); props.closeFunction() }}>
						Not today
					</button>
				</div>
			</form>
		</div>
	)
}

export default function VolumeAdjust(props) {
	const [modalOpen, setModalOpen] = useState(false);

	const closeFunction = () => setModalOpen(false);

	return (
		<div id="volume-adjust" onClick={() => {
			if (modalOpen === false) {
				setModalOpen(true)
			}
		}}>
			Adjust Volume
			<Modal
				isOpen={modalOpen}
				closeFunction={closeFunction}
			>
				{ modalOpen ? <VolumeAdjustModal
					closeFunction={closeFunction}
					getSelectedTracks={props.getSelectedTracks}
				/> : null }
			</Modal>
		</div>
	)
}
