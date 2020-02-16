import React, {useContext, useEffect, useState} from "react";
import {Api} from "../../../api";
import MiniPlayer from "../../playback-controls/mini-player/mini-player";
import {getDeviceId} from "../../../services/version";
import {SocketContext} from "../../../services/socket-provider";

export default function RemotePlayManagement() {
	const [devices, setDevices] = useState([]);
	const [forceRerender, setForceRerender] = useState(0);
	const [loading, setLoading] = useState(true);

	const socketContext = useContext(SocketContext);

	useEffect(() => {
		Api.get(`device/active?excluding-device=${getDeviceId()}`).then(devices => {
			setDevices(devices);
			setLoading(false);
		});

		// Though not "react-y", it is, imo, significantly simpler to use an interval and force an update here
		// to update all the progress bar estimates than to update the original state. Updating the original state
		// that we got from the server opens up possible race conditions as two things are now trying to update
		// state, and React updates state when it feels like it. So it's possible to queue up a derived-state update,
		// and a server-side important state update, and have that get stomped out by our derived state update
		const interval = setInterval(() => {
			setForceRerender(Math.random());
		}, 1000);

		return () => {
			clearInterval(interval);
		}
	}, []);

	const deviceIdToListeningState = {};
	Object.values(socketContext.nowListeningUsers).flat().forEach(listeningData => {
		deviceIdToListeningState[listeningData.deviceId] = listeningData;
	});

	const currentTime = Date.now();

	return <div id="remote-play-management">
		{devices.map(device => {
			const listeningState = deviceIdToListeningState[device.id] || {};
			const elapsedTime = currentTime - listeningState.lastUpdate;

			// We might only get updates every ~20 seconds or so. Estimate the play time
			// if the song is playing so the bar doesn't update so infrequently.
			const estimatedTimePlayed = listeningState.playing
				? listeningState.timePlayed + (elapsedTime / 1000)
				: listeningState.timePlayed;

			return <MiniPlayer
				key={device.id}
				title={device.deviceName}
				trackData={listeningState.trackData || {}}
				playing={listeningState.playing}
				volume={listeningState.volume}
				shuffling={listeningState.shuffling}
				repeating={listeningState.repeating}
				timePlayed={estimatedTimePlayed}
			/>
		})}
	</div>
}
