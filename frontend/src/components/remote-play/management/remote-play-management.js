import React, {useContext, useEffect, useState} from "react";
import {Api} from "../../../api";
import MiniPlayer from "../../playback-controls/mini-player/mini-player";
import {getDeviceId} from "../../../services/version";
import {SocketContext} from "../../../services/socket-provider";
import {RemotePlayType} from "../modal/remote-play-type";

const OVERRIDE_DURATION_MS = 5000;

export default function RemotePlayManagement() {
	const [devices, setDevices] = useState([]);
	const [forceRerender, setForceRerender] = useState(0);
	const [loading, setLoading] = useState(true);

	// We have an interesting problem to solve, in that we want this page to be an accurate reflection of what
	// the remote device is reporting, but we also want to have immediate feedback when we want the device to
	// change its state to something new. The compromise that is chosen here is that whenever we change the
	// state, keep an explicit override around for display purposes only, and delete it after a few seconds.
	// This offers immediate feedback, but won't hide a state change being unsuccessful, or another user
	// changing the state to something different after we did our own change.
	const [nowListeningOverrides, setNowListeningOverrides] = useState({});

	const socket = useContext(SocketContext);

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

	const setOverride = (deviceId, key, value) => {
		if (nowListeningOverrides[deviceId] === undefined) {
			nowListeningOverrides[deviceId] = {};
		}
		nowListeningOverrides[deviceId][key] = {
			value,
			expires: Date.now() + OVERRIDE_DURATION_MS
		};
		setForceRerender(Math.random());
	};

	const getDeviceValue = (deviceId, deviceState, key) => {
		if (
			nowListeningOverrides[deviceId] !== undefined
			&& nowListeningOverrides[deviceId][key] !== undefined
			&& nowListeningOverrides[deviceId][key].expires > Date.now()
		) {
			return nowListeningOverrides[deviceId][key].value;
		} else {
			return deviceState[key];
		}
	};

	const deviceIdToListeningState = {};
	Object.values(socket.nowListeningUsers).flat().forEach(listeningData => {
		deviceIdToListeningState[listeningData.deviceId] = listeningData;
	});

	const currentTime = Date.now();

	return <div id="remote-play-management">
		{devices.map(device => {
			const listeningState = deviceIdToListeningState[device.id] || {};
			const elapsedTime = currentTime - getDeviceValue(device.id, listeningState, 'lastTimeUpdate');

			// We might only get updates every ~20 seconds or so. Estimate the play time
			// if the song is playing so the bar doesn't update so infrequently.
			const timePlayed = getDeviceValue(device.id, listeningState, 'timePlayed');
			const estimatedTimePlayed = listeningState.playing
				? timePlayed + (elapsedTime / 1000)
				: timePlayed;

			return <MiniPlayer
				key={device.id}
				title={device.deviceName}
				trackData={listeningState.trackData || {}}
				playing={getDeviceValue(device.id, listeningState, 'playing')}
				volume={getDeviceValue(device.id, listeningState, 'volume')}
				muted={getDeviceValue(device.id, listeningState, 'muted')}
				shuffling={getDeviceValue(device.id, listeningState, 'shuffling')}
				repeating={getDeviceValue(device.id, listeningState, 'repeating')}
				timePlayed={estimatedTimePlayed}
				onPauseChange={() => {
					setOverride(device.id, 'playing', !getDeviceValue(device.id, listeningState, 'playing'));
					socket.sendRemotePlayEvent(
						listeningState.playing ? RemotePlayType.PAUSE : RemotePlayType.PLAY,
						device.id
					);
				}}
				onShuffleChange={() => {
					const currentState = getDeviceValue(device.id, listeningState, 'shuffling');
					setOverride(device.id, 'shuffling', !currentState);
					socket.sendRemotePlayEvent(
						currentState ? RemotePlayType.SHUFFLE_DISABLE : RemotePlayType.SHUFFLE_ENABLE,
						device.id
					);
				}}
				onRepeatChange={() => {
					const currentState = getDeviceValue(device.id, listeningState, 'repeating');
					setOverride(device.id, 'repeating', !currentState);
					socket.sendRemotePlayEvent(
						currentState ? RemotePlayType.REPEAT_DISABLE : RemotePlayType.REPEAT_ENABLE,
						device.id
					);
				}}
				onMuteChange={() => {
					const currentState = getDeviceValue(device.id, listeningState, 'muted');
					setOverride(device.id, 'muted', !currentState);
					socket.sendRemotePlayEvent(
						currentState ? RemotePlayType.UNMUTE : RemotePlayType.MUTE,
						device.id
					);
				}}
				onTimeChange={(newTimePercent, isHeld) => {
					const newTime = newTimePercent * listeningState.trackData.duration;
					setOverride(device.id, 'timePlayed', newTime);
					setOverride(device.id, 'lastTimeUpdate', Date.now());
					if (!isHeld) {
						socket.sendRemotePlayEvent(
							RemotePlayType.SEEK,
							device.id,
							{ newFloatValue: newTime }
						)
					}
				}}
				onVolumeChange={(newVolume, isHeld) => {
					setOverride(device.id, 'volume', newVolume);
					if (!isHeld) {
						socket.sendRemotePlayEvent(
							RemotePlayType.SET_VOLUME,
							device.id,
							{ newFloatValue: newVolume }
						)
					}
				}}
				onPlayNext={() => {
					socket.sendRemotePlayEvent(RemotePlayType.PLAY_NEXT, device.id)
				}}
				onPlayPrevious={() => {
					socket.sendRemotePlayEvent(RemotePlayType.PLAY_PREVIOUS, device.id)
				}}
			/>
		})}
	</div>
}
