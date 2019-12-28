import React, {useContext, useEffect, useState} from 'react';
import {Modal} from "../modal/modal";
import {UserContext} from "../../services/user-provider";
import {Api} from "../../api";
import {formatDate} from "../../formatters";
import {toast} from "react-toastify";

function TrackHistoryModal(props) {
	const userContext = useContext(UserContext);

	const [selectedUserId, setSelectedUserId] = useState(userContext.ownUser.id);
	const [startDate, setStartDate] = useState(formatDateWithDayOffset(-30));
	const [endDate, setEndDate] = useState(formatDateWithDayOffset(0));
	const [pendingDeleteData, setPendingDeleteData] = useState(null);
	const [historyData, setHistoryData] = useState([]);

	useEffect(() => {
		Api.get('track-history', {
			userId: selectedUserId,
			startDate: (new Date(startDate)).getTime(),
			endDate: (new Date(endDate)).getTime()
		}).then(history => {
			setHistoryData(history);
		})
	}, [selectedUserId, startDate, endDate]);

	const deleteHistory = () => {
		Api.delete('track-history/' + pendingDeleteData.id).then(() => {
			const newHistoryData = historyData.filter(datum => datum.id !== pendingDeleteData.id);
			setPendingDeleteData(null);
			setHistoryData(newHistoryData);
			toast.success('The track history was deleted successfully');
		}).catch(res => {
			console.error(res);
			toast.error('The deletion of the track history failed');
		});
	};

	return (
		<div id="track-history-modal" className="full-screen">
			<h2>Track History</h2>

			<Modal
				isOpen={pendingDeleteData !== null}
				fullScreen={false}
				closeFunction={() => setPendingDeleteData(null)}
			>
				<div>
					Are you sure you want to delete a piece of history?
				</div>
				{
					pendingDeleteData
						? <div>({pendingDeleteData.trackArtist} - {pendingDeleteData.trackName})</div>
						: <div/>
				}
				<div className="flex-between confirm-modal-buttons">
					<button onMouseDown={deleteHistory}>Let's get it over with</button>
					<button onMouseDown={() => setPendingDeleteData(null)}>Wait. I need to reconsider</button>
				</div>
			</Modal>

			<div className="content-wrapper flex-between">
				<div className="left-nav">
					<div>
						<div className="top-label">
							<label>User</label>
							<select value={selectedUserId} onChange={e => setSelectedUserId(e.target.value)}>
								<option value={userContext.ownUser.id}>
									{ userContext.ownUser.username }
								</option>
								<option value="" disabled="disabled">───────────</option>
								{ userContext.otherUsers.map(user => (
									<option value={user.id} key={user.id}>
										{ user.username }
									</option>
								))}
							</select>
						</div>
						<div className="top-label">
							<label>Start</label>
							<input
								type="date"
								value={startDate}
								onChange={e => setStartDate(e.target.value)}
							/>
						</div>
						<div className="top-label">
							<label>End</label>
							<input
								type="date"
								value={endDate}
								onChange={e => setEndDate(e.target.value)}
							/>
						</div>
					</div>

					<div>
						<button onClick={props.closeFunction}>Back</button>
					</div>
				</div>

				<div className="flex-grow overflow-y-auto">
					<table className="stats-table auto-margin">
						<thead>
						<tr>
							<th><div>Name</div></th>
							<th>Artist</th>
							<th>Album</th>
							<th>Listened On</th>
							<th>Device</th>
							<th/>
						</tr>
						</thead>
						<tbody>
						{historyData.map(historyDatum => (
							<tr key={historyDatum.id} className="history-row">
								<td>{historyDatum.trackName}</td>
								<td>{historyDatum.trackArtist}</td>
								<td>{historyDatum.trackAlbum}</td>
								<td>{formatDate(historyDatum.listenedDate, true)}</td>
								<td>{historyDatum.deviceName}</td>
								<td onMouseDown={() => setPendingDeleteData(historyDatum)}>
									<i className="fas fa-times cursor-pointer"/>
								</td>
							</tr>
						))}
						</tbody>
					</table>
				</div>
			</div>
		</div>
	)
}

// Separate out the modal and the content. Keeps the modal content from ever trying to render / run
// until it is actually clicked on. Makes assigning defaults and dealing with hooks easier since we
// know that the site will have been initialized by that time.
export default function TrackHistory() {
	const [modalOpen, setModalOpen] = useState(false);

	const closeFunction = () => setModalOpen(false);

	return (
		<div id="track-history" onMouseDown={() => setModalOpen(true)}>
			Track History
			<Modal
				isOpen={modalOpen}
				fullScreen={true}
				closeFunction={closeFunction}
			>
				{ modalOpen ? <TrackHistoryModal closeFunction={closeFunction}/> : <div/> }
			</Modal>
		</div>
	)
}

function formatDateWithDayOffset(dateOffset) {
	const d = new Date();
	d.setDate(d.getDate() + dateOffset);

	const month = (d.getMonth() < 9 ? '0' : '') + (d.getMonth() + 1);
	const day = (d.getDate() < 9 ? '0' : '') + d.getDate();
	const year = d.getFullYear();

	return [year, month, day].join('-');
}
