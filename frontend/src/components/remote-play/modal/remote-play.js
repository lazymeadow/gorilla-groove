import React, {useContext, useEffect, useState} from 'react';
import {Modal} from "../../modal/modal";
import {toast} from "react-toastify";
import {LoadingSpinner} from "../../loading-spinner/loading-spinner";
import {RemotePlayType} from "./remote-play-type";
import {SocketContext} from "../../../services/socket-provider";
import {UserContext} from "../../../services/user-provider";
import {DeviceContext} from "../../../services/device-provider";

function RemotePlayModal(props) {
	const [loading, setLoading] = useState(true);

	const socket = useContext(SocketContext);
	const userContext = useContext(UserContext);
	const deviceContext = useContext(DeviceContext);

	useEffect(() => {
		deviceContext.loadOtherDevices().then(() => {
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

	const sendRemoteAction = e => {
		const selectEl = document.getElementById('remote-play-selection');
		const targetId = selectEl.options[selectEl.selectedIndex].value;

		socket.sendRemotePlayEvent(
			props.playType,
			targetId,
			{ trackIds: props.getSelectedTracks().map(track => track.id) }
		);

		toast.success('Remote play action sent');
		props.closeFunction(e);
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
						{ deviceContext.otherDevices.map(device =>
							<option key={device.id} value={device.id}>
								{ userContext.ownUser.id === device.userId
									? device.deviceName
									: `${device.deviceName} (${device.userName}'s device)`
								}
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

	const closeFunction = e => {
		if (e) {
			e.stopPropagation();
		}
		setModalOpen(false)
	};

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
