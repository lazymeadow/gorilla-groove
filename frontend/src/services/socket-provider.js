import React from "react";
import {isLoggedIn} from "../util";
import {Api} from "../api";
import {getDeviceId} from "./version";
import {RemotePlayType} from "../components/remote-play/modal/remote-play-type";

export const SocketContext = React.createContext();

let lastEventId = -1;

export class SocketProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			nowListeningUsers: {},

			connectToSocket: (...args) => this.connectToSocket(...args),
			sendPlayEvent: (...args) => this.sendPlayEvent(...args),
			sendRemotePlayEvent: (...args) => this.sendRemotePlayEvent(...args)
		}
	}

	componentDidMount() {
		window.addEventListener('beforeunload', this.disconnectSocket.bind(this));
	}

	fetchLatestData() {
		Api.get(`event/device-id/${getDeviceId()}`, { lastEventId }).then(result => {
			lastEventId = result.lastEventId;

			if (result.eventType === EventType.NOW_PLAYING) {
				this.handleNowListeningMessage(result);
			} else if (result.eventType === EventType.REMOTE_PLAY) {
				this.handleRemotePlayMessage(result);
			}

			this.fetchLatestData();
		}).catch(() => {
			setTimeout(this.fetchLatestData.bind(this), 2000);
		});
	}

	handleNowListeningMessage(message) {
		this.setState({ nowListeningUsers: message.currentlyListeningUsers });
	}

	handleRemotePlayMessage(message) {
		switch (message.remotePlayAction) {
			case RemotePlayType.PLAY_SET_SONGS:
				return this.props.musicContext.playTracks(message.tracks);
			case RemotePlayType.ADD_SONGS_NEXT:
				return this.props.musicContext.playTracksNext(message.tracks);
			case RemotePlayType.ADD_SONGS_LAST:
				return this.props.musicContext.playTracksLast(message.tracks);
			case RemotePlayType.PLAY:
				return this.props.playbackContext.setProviderState({ isPlaying: true });
			case RemotePlayType.PAUSE:
				return this.props.playbackContext.setProviderState({ isPlaying: false });
			case RemotePlayType.SHUFFLE_ENABLE:
				this.sendPlayEvent({ isShuffling: true });
				return this.props.musicContext.setShuffleSongs(true);
			case RemotePlayType.SHUFFLE_DISABLE:
				this.sendPlayEvent({ isShuffling: false });
				return this.props.musicContext.setShuffleSongs(false);
			case RemotePlayType.REPEAT_ENABLE:
				this.sendPlayEvent({ isRepeating: true });
				return this.props.musicContext.setRepeatSongs(true);
			case RemotePlayType.REPEAT_DISABLE:
				this.sendPlayEvent({ isRepeating: false });
				return this.props.musicContext.setRepeatSongs(false);
			case RemotePlayType.SET_VOLUME:
				this.sendPlayEvent({ volume: message.newFloatValue });
				return this.props.playbackContext.setVolume(message.newFloatValue);
			case RemotePlayType.MUTE:
				this.sendPlayEvent({ muted: true });
				return this.props.playbackContext.setMuted(true);
			case RemotePlayType.UNMUTE:
				this.sendPlayEvent({ muted: false });
				return this.props.playbackContext.setMuted(false);
			case RemotePlayType.SEEK:
				return this.props.playbackContext.setProviderState({ timePlayedOverride: message.newFloatValue });
		}
	}

	connectToSocket() {
		// Avoid sending a new connection on logout / login
		// If our last update was not -1 then it means we're already looking for new data
		if (!isLoggedIn() || lastEventId !== -1) {
			return;
		}

		this.fetchLatestData();
	}

	disconnectSocket() {
		this.sendPlayEvent({ disconnected: true });
	}

	sendPlayEvent(data) {
		const optionalKeys = ['isShuffling', 'isRepeating', 'timePlayed', 'isPlaying', 'volume',
			'removeTrack', 'disconnected', 'muted'];
		const payload = {
			deviceId: getDeviceId()
		};

		if (data.track !== undefined) {
			if (data.track === null) {
				payload.trackId = null;
			} else {
				payload.trackId = data.track.id;
			}
		}

		optionalKeys.forEach(key => {
			if (data[key] !== undefined) {
				payload[key] = data[key];
			}
		});

		Api.post('event/NOW_PLAYING', payload);
	}

	sendRemotePlayEvent(eventType, targetDeviceId, optionalParams) {
		const data = optionalParams || {};

		const payload = {
			remotePlayAction: eventType,
			deviceId: getDeviceId(),
			targetDeviceId
		};

		if (data.trackIds !== undefined) {
			payload.trackIds = data.trackIds
		}
		if (data.newFloatValue !== undefined) {
			payload.newFloatValue = data.newFloatValue;
		}

		return Api.post('event/REMOTE_PLAY', payload);
	}

	render() {
		return (
			<SocketContext.Provider value={this.state}>
				{this.props.children}
			</SocketContext.Provider>
		)
	}
}

const EventType = Object.freeze({
	NOW_PLAYING: 'NOW_PLAYING',
	REMOTE_PLAY: 'REMOTE_PLAY'
});
