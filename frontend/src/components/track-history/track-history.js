import React, {useContext, useEffect, useState} from 'react';
import {Modal} from "../modal/modal";
import {UserContext} from "../../services/user-provider";
import {Api} from "../../api";

function TrackHistoryModal() {
	const userContext = useContext(UserContext);

	const [selectedUserId, setSelectedUserId] = useState(userContext.ownUser.id);
	console.log(selectedUserId, userContext.ownUser);
	const [startDate, setStartDate] = useState(formatDateWithDayOffset(-30));
	const [endDate, setEndDate] = useState(formatDateWithDayOffset(0));
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

	return (
		<div id="track-history-modal" className="full-screen">
			<h2>Track History</h2>
			<div className="content-wrapper flex-between">
				<div className="left-nav">
					<div className="top-label">
						<label>User</label>
						<select value={selectedUserId} onChange={e => setSelectedUserId(e.target.value)}>
							<option value={userContext.ownUser.id}>
								{ userContext.ownUser.username }
							</option>
							<option value="" disabled="disabled">─────────────</option>
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

				<div className="flex-grow overflow-y-auto">
					<table className="auto-margin">
						<thead>
						<tr>
							<th>Name</th>
							<th>Artist</th>
							<th>Album</th>
							<th>Listened On</th>
						</tr>
						</thead>
						<tbody>
						{historyData.map(historyDatum => (
							<tr key={historyDatum.trackHistoryId}>
								<td>{historyDatum.trackName}</td>
								<td>{historyDatum.trackArtist}</td>
								<td>{historyDatum.trackAlbum}</td>
								<td>{historyDatum.listenedDate}</td>
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

	return (
		<div id="track-history" onMouseDown={() => setModalOpen(true)}>
			Track History
			<Modal
				isOpen={modalOpen}
				fullScreen={true}
				closeFunction={() => setModalOpen(false)}
			>
				{ modalOpen ? <TrackHistoryModal/> : <div/> }
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
