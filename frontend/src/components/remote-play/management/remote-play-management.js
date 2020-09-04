import React, {useContext, useEffect, useState} from "react";
import MiniPlayer from "../../playback-controls/mini-player/mini-player";
import {SocketContext} from "../../../services/socket-provider";
import {RemotePlayType} from "../modal/remote-play-type";
import {UserContext} from "../../../services/user-provider";
import {LoadingSpinner} from "../../loading-spinner/loading-spinner";
import {Modal} from "../../modal/modal";
import {toast} from "react-toastify";
import {DeviceContext} from "../../../services/device-provider";

const OVERRIDE_DURATION_MS = 5000;

// We have an interesting problem to solve, in that we want this page to be an accurate reflection of what
// the remote device is reporting, but we also want to have immediate feedback when we want the device to
// change its state to something new. The compromise that is chosen here is that whenever we change the
// state, keep an explicit override around for display purposes only, and delete it after a few seconds.
// This offers immediate feedback, but won't hide a state change being unsuccessful, or another user
// changing the state to something different after we did our own change.
const nowListeningOverrides = {};

export default function RemotePlayManagement() {
	const [forceRerender, setForceRerender] = useState(0);
	const [loading, setLoading] = useState(true);
	const [partyOptionsModalOpen, setPartyOptionsModalOpen] = useState(false);
	const [modifyingParty, setModifyingParty] = useState(false);

	const socket = useContext(SocketContext);
	const userContext = useContext(UserContext);
	const deviceContext = useContext(DeviceContext);

	useEffect(() => {
		deviceContext.loadOtherDevices().then(() => {
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
	Object.values(socket.nowListeningUsers)
		.map(it => Object.values(it))
		.flat()
		.forEach(listeningData => {
			deviceIdToListeningState[listeningData.deviceId] = listeningData;
		});

	const setPartyMode = isSet => {
		let msUntilExpiration = null;
		let userIds = [];

		if (isSet) {
			const durationSelectEl = document.getElementById('party-duration-select');
			const durationMinutes = durationSelectEl.options[durationSelectEl.selectedIndex].value;

			if (durationMinutes > 0) {
				msUntilExpiration = durationMinutes * 60 * 1000
			}

			const userSelectEl = document.getElementById('party-user-select');
			userIds = [...userSelectEl.options]
				.filter(it => it.selected)
				.map(it => it.value);

			if (userIds.length === 0) {
				toast.info("Select at least one other user for your party, or it isn't much of a party");
				return;
			}
		}

		setModifyingParty(true);
		deviceContext.setPartyMode(isSet, userIds, msUntilExpiration).then(() => {
			setModifyingParty(false);
			setPartyOptionsModalOpen(false);
		}).catch(res => {
			console.error(res);
			toast.error('Failed to issue remote play command')
		});
	};

	const currentTime = Date.now();

	return <div id="remote-play-management">
		<div>
			<div id="party-mode-button" className="auto-margin text-center">
				{ deviceContext.isInPartyMode()
					? <div className="inner-text full-dimensions" onClick={() => setPartyMode(false)}>
						End the Party
						<div className="small-text">
							({ formatTimeLeft(deviceContext.ownDevice.partyEnabledUntil) } left)
						</div>
					</div>
					: <div className="inner-text animation-rainbow full-dimensions" onClick={() => setPartyOptionsModalOpen(true)}>
						Start A Party
					</div>
				}
				<LoadingSpinner visible={modifyingParty}/>
				<Modal
					isOpen={partyOptionsModalOpen}
					closeFunction={() => setPartyOptionsModalOpen(false)}
				>
					<div id="party-option-modal">
						<h2 className="text-center">Let's Party</h2>
						<hr/>

						<form onSubmit={e => { e.preventDefault(); setPartyMode(true); }}>
							<div>
								How long will you be Partying?
								<select id="party-duration-select">
									<option value="30">30 minutes</option>
									<option value="60">1 hour</option>
									<option value="120">2 hours</option>
									<option value="240">4 hours</option>
									<option value="480">8 hours</option>
									<option value="-1">The party never ends</option>
								</select>
							</div>

							<div>
								Who is joining your Party?
								<select id="party-user-select" multiple>
									{ userContext.otherUsers.map(user =>
										<option key={user.id} value={user.id}>{user.username}</option>
									)}
								</select>
							</div>

							<div className="text-center">
								<button type="submit">Uhn Tiss Uhn Tiss</button>
							</div>
						</form>
					</div>
				</Modal>
			</div>
		</div>

		<hr/>

		<div className="device-list">
			{ deviceContext.otherDevices.length === 0 ? <div>No other active devices found</div> : null }

			{ deviceContext.otherDevices.map(device => {
				const listeningState = deviceIdToListeningState[device.id] || {};
				const elapsedTime = currentTime - getDeviceValue(device.id, listeningState, 'lastTimeUpdate');

				// We might only get updates every ~20 seconds or so. Estimate the play time
				// if the song is playing so the bar doesn't update so infrequently.
				const timePlayed = getDeviceValue(device.id, listeningState, 'timePlayed');
				const estimatedTimePlayed = listeningState.isPlaying
					? timePlayed + (elapsedTime / 1000)
					: timePlayed;

				const title = device.userId === userContext.ownUser.id
					? device.deviceName
					: `${device.deviceName} (${device.userName}'s device)`;

				return <MiniPlayer
					key={device.id}
					title={title}
					trackData={listeningState.trackData || {}}
					playing={getDeviceValue(device.id, listeningState, 'isPlaying')}
					volume={getDeviceValue(device.id, listeningState, 'volume')}
					muted={getDeviceValue(device.id, listeningState, 'muted')}
					shuffling={getDeviceValue(device.id, listeningState, 'isShuffling')}
					repeating={getDeviceValue(device.id, listeningState, 'isRepeating')}
					timePlayed={estimatedTimePlayed}
					onPauseChange={() => {
						setOverride(device.id, 'isPlaying', !getDeviceValue(device.id, listeningState, 'isPlaying'));
						socket.sendRemotePlayEvent(
							listeningState.isPlaying ? RemotePlayType.PAUSE : RemotePlayType.PLAY,
							device.id
						);
					}}
					onShuffleChange={() => {
						const currentState = getDeviceValue(device.id, listeningState, 'isShuffling');
						setOverride(device.id, 'isShuffling', !currentState);
						socket.sendRemotePlayEvent(
							currentState ? RemotePlayType.SHUFFLE_DISABLE : RemotePlayType.SHUFFLE_ENABLE,
							device.id
						);
					}}
					onRepeatChange={() => {
						const currentState = getDeviceValue(device.id, listeningState, 'isRepeating');
						setOverride(device.id, 'isRepeating', !currentState);
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
						const newTime = newTimePercent * listeningState.trackData.length;
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
	</div>
}

function formatTimeLeft(timeTarget) {
	const msLeft = new Date(timeTarget) - new Date();
	const secondsLeft = Math.round(msLeft / 1000);
	const minutesLeft = Math.round(secondsLeft / 60);
	const hoursLeft = Math.round(minutesLeft / 60);

	if (minutesLeft <= 90) {
		return minutesLeft + (minutesLeft > 1 ? ' minutes' : ' minute');
	}

	if (hoursLeft > 10000) {
		return '∞ hours'
	}

	return hoursLeft + (hoursLeft > 1 ? ' hours' : 'hours');
}
