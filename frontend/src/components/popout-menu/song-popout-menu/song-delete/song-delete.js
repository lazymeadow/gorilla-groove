import React, {useContext, useState} from "react";
import {Modal} from "../../../modal/modal";
import {MusicContext} from "../../../../services/music-provider";
import {toast} from "react-toastify";

export default function SongDelete(props) {
	const [modalOpen, setModalOpen] = useState(false);
	const musicContext = useContext(MusicContext);
	const deleteSongs = (e) => {
		e.stopPropagation();
		const tracks = props.getSelectedTracks();
		musicContext.deleteTracks(tracks, false).then(() => {
			if (tracks.length === 1) {
				toast.success(`'${tracks[0].name}' was deleted`);
			} else {
				toast.success(`${tracks.length} tracks were deleted`);
			}
			setModalOpen(false);
		}).catch(error => {
			console.error(error);
			toast.error('Failed to delete the selected tracks');
		});
	};
	const getDeletionText = () => {
		const tracks = props.getSelectedTracks();
		if (tracks[0] !== undefined) {
			if (tracks.length === 1) {
				return `Are you sure you want to delete ${tracks[0].name} - ${tracks[0].artist}?`;
			} else {
				return `Are you sure you want to delete these ${tracks.length} tracks?`;
			}
		}
	};

	return (
		<div onClick={() => setModalOpen(true)}>
			<span>Delete</span>
			<Modal
				isOpen={modalOpen}
				closeFunction={() => setModalOpen(false)}
			>
				{getDeletionText()}
				<div className="flex-between confirm-modal-buttons">
					<button onClick={deleteSongs}>Yeet!</button>
					<button onClick={(e) => {
						e.stopPropagation();
						setModalOpen(false);
					}}>Gimme A Moment
					</button>
				</div>
			</Modal>
		</div>
	)


}
