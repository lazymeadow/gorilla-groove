import React, {useContext, useState} from 'react';
import {Modal} from "../modal/modal";
import {toast} from "react-toastify";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";

let wasOpen = false;

export default function MetadataRequest(props) {
	const [modalOpen, setModalOpen] = useState(false);
	const [metadataSettings, setMetadataSettings] = useState([
		{ metadataName: 'Album', overrideType: MetadataOverrideType.IF_EMPTY },
		{ metadataName: 'AlbumArt', overrideType: MetadataOverrideType.IF_EMPTY },
		{ metadataName: 'ReleaseYear', overrideType: MetadataOverrideType.IF_EMPTY },
		{ metadataName: 'TrackNumber', overrideType: MetadataOverrideType.IF_EMPTY }
	]);

	const [loading, setLoading] = useState(false);

	const selectedTracks = props.getSelectedTracks();

	const musicContext = useContext(MusicContext);

	const updateMetadataSettings = (name, newType) => {
		const index = metadataSettings.findIndex(data => data.metadataName === name);
		const newSettings = metadataSettings.slice(0);
		newSettings[index].overrideType = newType;

		setMetadataSettings(newSettings)
	};

	const setAllSettings = overrideType => {
		const newSettings = metadataSettings.map(setting => ({
			metadataName: setting.metadataName,
			overrideType: overrideType
		}));
		setMetadataSettings(newSettings);
	};

	const requestData = () => {
		setLoading(true);

		const params = { trackIds: selectedTracks.map(track => track.id)};
		metadataSettings.forEach(it => params['change' + it.metadataName] = it.overrideType);

		Api.post('track/data-update-request', params).then(res => {
			const successes = res.successfulUpdates;
			const failures = res.failedUpdateIds;
			successes.forEach(updatedTrack => {
				const existingTrack = selectedTracks.find(selectedTrack => selectedTrack.id === updatedTrack.id);
				existingTrack.album = updatedTrack.album;
				existingTrack.releaseYear = updatedTrack.releaseYear;
				existingTrack.trackNumber = updatedTrack.trackNumber;
			});

			const successMessage = `${successes.length} song${successes.length === 1 ? ' was' : 's were'} updated successfully`;
			if (successes.length && !failures.length) {
				toast.success(successMessage)
			}	else if (successes.length && failures.length) {
				const errorMessage = `\n\nHowever, the following songs failed to update:\n`;
				const failedSongMessage = failures.map(trackId => {
					const track = selectedTracks.find(it => it.id === trackId);
					return track.artist + ' - ' + track.name;
				}).join('\n');
				toast.info(successMessage + errorMessage + failedSongMessage);
			} else {
				if (failures.length > 1) {
					toast.error('Failed to find metadata for the selected songs')
				} else {
					toast.error(`Failed to find metadata for ${selectedTracks[0].artist} - ${selectedTracks[0].name}`)
				}
			}

			if (successes.length) {
				musicContext.forceTrackUpdate();
			}
		}).catch(err => {
			console.error(err);
			toast.error('An error was encountered requesting song metadata')
		}).finally(() => {
			setModalOpen(false);
			setLoading(false);
		})
	};

	const openModal = () => {
		const tracks = props.getSelectedTracks();
		const invalidTracks = tracks.filter(track => track.artist.trim() === '' || track.name.trim() === '');

		if (invalidTracks.length) {
			toast.info('Only tracks with both a name and an artist can be searched for metadata');
			return;
		}

		setModalOpen(true);
	};

	if (!modalOpen && wasOpen) {
		setAllSettings(MetadataOverrideType.IF_EMPTY);
	}

	wasOpen = modalOpen;

	return (
		<div onClick={openModal}>
			<span>Request Metadata</span>
			<Modal
				isOpen={modalOpen}
				closeFunction={() => setModalOpen(false)}
			>
				<div id="metadata-request-modal" className="p-relative">
					<LoadingSpinner visible={loading}/>
					<h2 className="text-center">Request Metadata</h2>
					<div className="metadata-request-content">
						Take the following pieces of Metadata:
						<hr/>
						<table className="full-width">
							<thead>
							<tr>
								<th/>
								<th className="clickable" onClick={() => setAllSettings(MetadataOverrideType.ALWAYS)}>Always</th>
								<th className="clickable" onClick={() => setAllSettings(MetadataOverrideType.IF_EMPTY)}>If Empty</th>
								<th className="clickable" onClick={() => setAllSettings(MetadataOverrideType.NEVER)}>Never</th>
							</tr>
							</thead>
							<tbody>
							{ metadataSettings.map((setting, index) => (
								<tr key={index}>
									<td>{ setting.metadataName }</td>

									<td className="text-center">
										<input
											type="radio"
											name={"metadata-" + setting.metadataName}
											value={MetadataOverrideType.ALWAYS}
											checked={setting.overrideType === MetadataOverrideType.ALWAYS}
											onChange={() => updateMetadataSettings(setting.metadataName, MetadataOverrideType.ALWAYS)}
										/>
									</td>

									<td className="text-center">
										<input
											type="radio"
											name={"metadata-" + setting.metadataName}
											value={MetadataOverrideType.IF_EMPTY}
											checked={setting.overrideType === MetadataOverrideType.IF_EMPTY}
											onChange={() => updateMetadataSettings(setting.metadataName, MetadataOverrideType.IF_EMPTY)}
										/>
									</td>

									<td className="text-center">
										<input
											type="radio"
											name={"metadata-" + setting.metadataName}
											value={MetadataOverrideType.NEVER}
											checked={setting.overrideType === MetadataOverrideType.NEVER}
											onChange={() => updateMetadataSettings(setting.metadataName, MetadataOverrideType.NEVER)}
										/>
									</td>
								</tr>
							))}
							</tbody>
						</table>
					</div>

					<div className="flex-between confirm-modal-buttons">
						<button type="button" onClick={e => { e.stopPropagation(); requestData(); }}>
							I like data
						</button>
						<button type="button" onClick={e => { e.stopPropagation(); setModalOpen(false); }}>
							I hate data
						</button>
					</div>
				</div>
			</Modal>
		</div>
	)
}

const MetadataOverrideType = Object.freeze({
	ALWAYS: "ALWAYS",
	IF_EMPTY: "IF_EMPTY",
	NEVER: "NEVER"
});
