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
			pendingRebroadcast: false,

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
		// Put it in a timeout so the context finishes its state set first and we have accurate state
		setTimeout(() => this.setState({ pendingRebroadcast: true }));

		switch (message.remotePlayAction) {
			case RemotePlayType.PLAY_SET_SONGS:
				return this.props.musicContext.playTracks(message.tracks);
			case RemotePlayType.ADD_SONGS_NEXT:
				return this.props.musicContext.playTracksNext(message.tracks);
			case RemotePlayType.ADD_SONGS_LAST:
				return this.props.musicContext.playTracksLast(message.tracks);
			case RemotePlayType.PLAY:
				return this.props.musicContext.setProviderState({ isPlaying: true });
			case RemotePlayType.PAUSE:
				return this.props.musicContext.setProviderState({ isPlaying: false });
			case RemotePlayType.SHUFFLE_ENABLE:
				return this.props.musicContext.setShuffleSongs(true);
			case RemotePlayType.SHUFFLE_DISABLE:
				return this.props.musicContext.setShuffleSongs(false);
			case RemotePlayType.REPEAT_ENABLE:
				return this.props.musicContext.setRepeatSongs(true);
			case RemotePlayType.REPEAT_DISABLE:
				return this.props.musicContext.setRepeatSongs(false);
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

	sendPlayEvent(data, clearRebroadcast) {
		const optionalKeys = ['isShuffling', 'isRepeating', 'timePlayed', 'isPlaying', 'volume', 'removeTrack', 'disconnected'];
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

		if (clearRebroadcast) {
			this.setState({ pendingRebroadcast: false });
		}
	}

	sendRemotePlayEvent(eventType, targetDeviceId, trackIds) {
		return Api.post('event/REMOTE_PLAY', {
			remotePlayAction: eventType,
			deviceId: getDeviceId(),
			targetDeviceId,
			trackIds
		});
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
