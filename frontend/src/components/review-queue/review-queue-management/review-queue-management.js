import React, {useContext, useEffect, useState} from 'react';
import {Modal} from "../../modal/modal";
import {toast} from "react-toastify";
import {LoadingSpinner} from "../../loading-spinner/loading-spinner";
import {Api} from "../../../api";
import {ReviewQueueContext} from "../../../services/review-queue-provider";
import {toTitleCaseFromSnakeCase} from "../../../formatters";

function ReviewQueueManagementModal(props) {
	const [loading, setLoading] = useState(true);

	const reviewQueueContext = useContext(ReviewQueueContext);

	useEffect(() => {
		reviewQueueContext.fetchReviewQueueSources().then(() => {
			setLoading(false);
		}).catch(() => {
			toast.error('Failed to load review sources!');
			setLoading(false);
		});
	}, []);

	const getSourceData = source => {
		let sourceData = null;
		if (source.sourceType === 'ARTIST') {
			sourceData = source.artistName;
		} else if (source.sourceType === 'YOUTUBE_CHANNEL') {
			sourceData = source.channelName;
		} else {
			throw 'Unrecognized source type! ' + source.sourceType;
		}

		return sourceData;
	};

	return (
		<div id="review-queue-management-modal" className="p-relative">
			<LoadingSpinner visible={loading}/>
			<h2 className="text-center ws-pre-line">Review Queue Management</h2>
			<AddNewSourceModal/>
			<table className="data-table full-width">
				<thead>
				<tr>
					<th>Type</th>
					<th>Source</th>
					<th/>
				</tr>
				</thead>
				<tbody>
				{reviewQueueContext.reviewQueueSources.map(source =>
					<tr key={source.id} className="">
						<td>{toTitleCaseFromSnakeCase(source.sourceType)}</td>
						<td>{getSourceData(source)}</td>
						<td className="">
							<i className="fas fa-times" title="Delete" onClick={() => {
								console.log('delete');
							}}/>
						</td>
					</tr>
				)}
				</tbody>
			</table>

			<div className="flex-between confirm-modal-buttons">
				<button type="button" onClick={() => {}}>
					Yes
				</button>
				<button type="button" onClick={e => { e.stopPropagation() }}>
					No
				</button>
			</div>
		</div>
	)
}

function AddNewSourceModal() {
	const [modalOpen, setModalOpen] = useState(false);
	const [loading, setLoading] = useState(false);
	const [selectedSourceType, setSelectedSourceType] = useState(ReviewSourceType.ARTIST);
	const [reviewQueueInput, setReviewQueueInput] = useState('');

	const subscribe = () => {
		if (reviewQueueInput.trim().length === 0) {
			return;
		}

		setLoading(true);

		if (selectedSourceType === ReviewSourceType.ARTIST) {
			Api.post('review-queue/subscribe/artist', { artistName: reviewQueueInput }).then(res => {
				console.log('First');
				setModalOpen(false);
			}).catch(err => {
				const error = JSON.parse(err);
				if (error.status === 400) {
					toast.error('You are already subscribed to this artist!');
				} else if (error.possibleMatches) {
					if (error.possibleMatches.length === 0) {
						toast.error(`No artist named ${reviewQueueInput} found on Spotify`)
					} else {
						toast.error('Need to handle this');
					}
				} else {
					toast.error('Uh oh');
				}
				setLoading(false);
			})
		} else {
			setLoading(false);
			throw 'Unsupported review source type!'
		}
	};

	return (
		<div id="add-new-source" className="text-center">
			<button id="add-review-source-button" onClick={() => { setModalOpen(true) }}>Add Review Source</button>
			<Modal
				isOpen={modalOpen}
				closeFunction={() => { setModalOpen(false) }}
			>
				<div id="add-new-source-modal" className="p-relative">
					<LoadingSpinner visible={loading}/>
					<h2 className="text-center">Add Review Source</h2>
					<div className="text-center">
						<label>Source Type</label>
						<select
							id="review-queue-type-select"
							value={selectedSourceType}
							onChange={e => setSelectedSourceType(e.target.value)}
						>
							<option value={ReviewSourceType.ARTIST}>Artist</option>
							<option value={ReviewSourceType.YOUTUBE_CHANNEL}>YouTube Channel</option>
						</select>
					</div>

					<hr/>

					<div className="flex-between">
						<label htmlFor="review-queue-input">
							{ selectedSourceType === ReviewSourceType.ARTIST ? 'Artist Name' : 'Channel or User URL' }
						</label>
						<input
							id="review-queue-input"
							className="flex-grow"
							value={reviewQueueInput}
							onChange={e => setReviewQueueInput(e.target.value)}
							placeholder={selectedSourceType === ReviewSourceType.ARTIST
								? 'Alestorm'
								: 'https://www.youtube.com/user/MrSuicideSheep'
							}
						/>
					</div>

					<div className="text-center">
						<button id="create-review-source-button" onClick={subscribe}>Create Review Source</button>
					</div>
				</div>
			</Modal>
		</div>
	)
}

export default function ReviewQueueManagement() {
	const [modalOpen, setModalOpen] = useState(false);

	const closeFunction = () => setModalOpen(false);

	return (
		<div id="review-queue-management" onClick={e => { e.stopPropagation(); setModalOpen(true) }}>
			<i className="fas fa-edit edit-review-queue hoverable"/>
			<Modal
				isOpen={modalOpen}
				closeFunction={closeFunction}
			>
				{ modalOpen ? <ReviewQueueManagementModal closeFunction={closeFunction}/> : null }
			</Modal>
		</div>
	)
}

export const ReviewSourceType = Object.freeze({
	USER_RECOMMEND: '0',
	YOUTUBE_CHANNEL: '1',
	ARTIST: '2'
});
