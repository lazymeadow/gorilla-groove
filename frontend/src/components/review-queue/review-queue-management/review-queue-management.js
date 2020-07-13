import React, {useContext, useEffect, useState} from 'react';
import {Modal} from "../../modal/modal";
import {toast} from "react-toastify";
import {LoadingSpinner} from "../../loading-spinner/loading-spinner";
import {Api} from "../../../api";
import {UserContext} from "../../../services/user-provider";
import {ReviewQueueContext} from "../../../services/review-queue-provider";
import {toTitleCaseFromSnakeCase} from "../../../formatters";

function ReviewQueueManagementModal(props) {
	const [modalOpen, setModalOpen] = useState(false);
	const [loading, setLoading] = useState(true);

	const reviewQueueContext = useContext(ReviewQueueContext);

	useEffect(() => {
		reviewQueueContext.fetchReviewQueueSources().then(() => {
			setLoading(false);
		}).catch(() => {
			toast.error('Failed to load review sources!')
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
			<h2 className="text-center">Review Queue Management</h2>
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
	return (
		<div className="text-center">
			<select id="recommend-to-select">
				<option value="1">1</option>
			</select>
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
