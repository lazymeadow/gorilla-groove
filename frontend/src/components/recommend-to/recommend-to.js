import React, {useContext, useState} from 'react';
import {Modal} from "../modal/modal";
import {toast} from "react-toastify";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {Api} from "../../api";
import {UserContext} from "../../services/user-provider";

function RecommendToModal(props) {
	const [loading, setLoading] = useState(false);

	const userContext = useContext(UserContext);

	const tracks = props.getSelectedTracks();

	const recommendSongs = () => {
		const selectEl = document.getElementById('recommend-to-select');
		const targetUserId = selectEl.options[selectEl.selectedIndex].value;

		setLoading(true);

		Api.post('review-queue/recommend', {
			targetUserId: targetUserId,
			trackIds: tracks.map(it => it.id)
		}).then(() => {
			props.closeFunction();
			const track = tracks.length === 1 ? 'Track' : 'Tracks';
			toast.success(`${track} recommended successfully`)
		}).catch(e => {
			const error = JSON.parse(e);
			if (error.message) {
				toast.error(error.message);
			} else {
				console.error(e);
				toast.error('Failed to recommend the selected songs');
			}
			setLoading(false);
		});
	};

	const addText = tracks.length === 1 ? 'song' : `${tracks.length} songs`;

	return (
		<div id="recommend-to-modal" className="p-relative">
			<LoadingSpinner visible={loading}/>
			<h2 className="text-center">Recommend Song</h2>
			Add the selected { addText } to the following user's Review Queue?

			<div className="text-center">
				<select id="recommend-to-select">
					{ userContext.otherUsers.map(user =>
						<option key={user.id} value={user.id}>{user.username}</option>
					)}
				</select>
			</div>

			<div className="flex-between confirm-modal-buttons">
				<button type="button" onClick={recommendSongs}>
					They'll definitely like this
				</button>
				<button type="button" onClick={e => { e.stopPropagation(); props.closeFunction() }}>
					No, I'll keep this one to myself
				</button>
			</div>
		</div>
	)
}

export default function RecommendTo(props) {
	const [modalOpen, setModalOpen] = useState(false);

	const closeFunction = () => setModalOpen(false);

	return (
		<div id="recommend-to" onClick={() => setModalOpen(true)}>
			Recommend To...
			<Modal
				isOpen={modalOpen}
				closeFunction={closeFunction}
			>
				{ modalOpen ? <RecommendToModal
					closeFunction={closeFunction}
					getSelectedTracks={props.getSelectedTracks}
				/> : null }
			</Modal>
		</div>
	)
}
