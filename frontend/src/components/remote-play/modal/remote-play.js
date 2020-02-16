import React, {useContext, useEffect, useState} from 'react';
import {Modal} from "../../modal/modal";
import {Api} from "../../../api";
import {toast} from "react-toastify";
import {getDeviceId} from "../../../services/version";
import {LoadingSpinner} from "../../loading-spinner/loading-spinner";
import {RemotePlayType} from "./remote-play-type";
import {SocketContext} from "../../../services/socket-provider";

function RemotePlayModal(props) {
	const [devices, setDevices] = useState([]);
	const [loading, setLoading] = useState(true);

	const socket = useContext(SocketContext);

	useEffect(() => {
		Api.get(`device/active?excluding-device=${getDeviceId()}`).then(devices => {
			setDevices(devices);
			setLoading(false);
		});
	}, []);

	const getPlayString = () => {
		switch(props.playType) {
			case RemotePlayType.PLAY_SET_SONGS:
				return 'Play songs on:';
			case RemotePlayType.ADD_SONGS_NEXT:
				return 'Play songs next on:';
			case RemotePlayType.ADD_SONGS_LAST:
				return 'Play songs last on:';
		}
	};

	const sendRemoteAction = () => {
		const selectEl = document.getElementById('remote-play-selection');
		const targetId = selectEl.options[selectEl.selectedIndex].value;

		socket.sendRemotePlayEvent(
			props.playType,
			targetId,
			props.getSelectedTracks().map(track => track.id)
		).then(() => {
			toast.success('Remote play action sent');
			props.closeFunction();
		});
	};

	return (
		<div onKeyDown={e => e.nativeEvent.propagationStopped = true}>
			<div id="remote-play-modal" className="p-relative text-center">
				<h2>Remote Play</h2>

				<hr/>

				<div>
					{ getPlayString() }
				</div>
				<div>
					<select id="remote-play-selection">
						{ devices.map(device =>
							<option key={device.id} value={device.id}>
								{device.deviceName}
							</option>
						)}
					</select>
				</div>

				<div>
					<button onClick={sendRemoteAction}>Groove Remotely</button>
				</div>

				<LoadingSpinner visible={loading}/>
			</div>
		</div>
	)
}

export default function RemotePlay(props) {
	const [modalOpen, setModalOpen] = useState(false);

	const closeFunction = () => setModalOpen(false);

	return (
		<div id="remote-play" onClick={() => setModalOpen(true)}>
			{ props.title }
			<Modal
				isOpen={modalOpen}
				closeFunction={closeFunction}
			>
				{ modalOpen ? <RemotePlayModal
					playType={props.playType}
					closeFunction={closeFunction}
					getSelectedTracks={props.getSelectedTracks}
				/> : null }
			</Modal>
		</div>
	)
}
