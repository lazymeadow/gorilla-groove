import React from "react";
import {isLoggedIn} from "../util";
import {Api} from "../api";
import {getDeviceIdentifier} from "./version";
import {RemotePlayType} from "../components/remote-play/modal/remote-play-type";
import {ReviewSourceType} from "../components/review-queue/review-queue-management/review-queue-management";
import {toast} from "react-toastify";

export const SocketContext = React.createContext();

let socket = null;
let forceDisconnect = false;

export class SocketProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			nowListeningUsers: {},
			onConnectedHandlers: [],
			isConnected: false,
			initialized: false,

			connectToSocket: (...args) => this.connectToSocket(...args),
			sendPlayEvent: (...args) => this.sendPlayEvent(...args),
			sendRemotePlayEvent: (...args) => this.sendRemotePlayEvent(...args),
			addOnConnectedHandler: (...args) => this.addOnConnectedHandler(...args)
		}
	}

	componentDidMount() {
		window.addEventListener('beforeunload', this.disconnectSocket.bind(this));
	}

	handleNowListeningMessage(data) {
			const newNowListeningUsers = Object.assign({}, this.state.nowListeningUsers);
			if (newNowListeningUsers[data.userId] === undefined) {
				newNowListeningUsers[data.userId] = [];
			}
			newNowListeningUsers[data.userId][data.deviceId] = data;

			this.setState({ nowListeningUsers: newNowListeningUsers })
	}

	handleRemotePlayMessage(message) {
		switch (message.remotePlayAction) {
			case RemotePlayType.PLAY_SET_SONGS:
				return this.props.musicContext.playTracks(message.tracks);
			case RemotePlayType.PLAY_NEXT:
				return this.props.musicContext.playNext();
			case RemotePlayType.PLAY_PREVIOUS:
				return this.props.musicContext.playPrevious();
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

	handleReviewQueueMessage(message) {
		this.props.reviewQueueContext.fetchReviewQueueSources();
		let toastMessage;
		switch (message.sourceType) {
			case ReviewSourceType.USER_RECOMMEND:
				const pluralTrack = message.count === 1 ? 'track' : 'tracks';
				toastMessage = `User ${message.sourceDisplayName} recommended you ${message.count} new ${pluralTrack}`;
				break;
			case ReviewSourceType.YOUTUBE_CHANNEL:
				const pluralVideo = message.count === 1 ? 'video' : 'videos';
				toastMessage = `YouTube channel ${message.sourceDisplayName} uploaded ${message.count} new ${pluralVideo}`;
				break;
			case ReviewSourceType.ARTIST:
				const pluralSong = message.count === 1 ? 'song' : 'songs';
				toastMessage = `Artist ${message.sourceDisplayName} released ${message.count} new ${pluralSong}`;
				break;
		}

		toast.info(toastMessage, { autoClose: 30000 });
	}

	connectToSocket() {
		// Avoid sending a new connection on logout / login
		if (!isLoggedIn()) {
			return;
		}

		forceDisconnect = false;

		const newSocket = new WebSocket(Api.getSocketUri());

		newSocket.onmessage = res => {
			const data = JSON.parse(res.data);

			switch (data.messageType) {
				case EventType.NOW_PLAYING: return this.handleNowListeningMessage(data);
				case EventType.REMOTE_PLAY: return this.handleRemotePlayMessage(data);
				case EventType.REVIEW_QUEUE: return this.handleReviewQueueMessage(data);
				case EventType.CONNECTION_ESTABLISHED: return console.info(data.message);
			}
		};
		newSocket.onclose = () => {
			this.setState({ isConnected: false });
			if (!forceDisconnect) {
				this.connectToSocket();
			}
		};
		newSocket.onopen = () => {
			this.state.onConnectedHandlers.forEach(it => it());
			this.setState({ isConnected: true, initialized: true });
		};
		socket = newSocket;
		this.setState({
			nowListeningUsers: {}
		});
	}

	disconnectSocket() {
		if (socket !== null) {
			// We have the socket automatically reconnect whenever it gets disconnected.
			// But in this case we want it to stay gone since we closed it explicitly
			forceDisconnect = true;
			socket.close();
		}
	}

	sendPlayEvent(data) {
		if (!socket) {
			console.debug("No socket defined. Not sending play event");
			return;
		}

		const optionalKeys = ['isShuffling', 'isRepeating', 'timePlayed', 'isPlaying', 'volume', 'removeTrack', 'muted'];
		const payload = {
			messageType: EventType.NOW_PLAYING,
			deviceId: getDeviceIdentifier()
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

		this.sendSocketData(payload);
	}

	sendRemotePlayEvent(eventType, targetDeviceId, optionalParams) {
		const data = optionalParams || {};

		const payload = {
			messageType: EventType.REMOTE_PLAY,
			remotePlayAction: eventType,
			deviceId: getDeviceIdentifier(),
			targetDeviceId
		};

		if (data.trackIds !== undefined) {
			payload.trackIds = data.trackIds
		}
		if (data.newFloatValue !== undefined) {
			payload.newFloatValue = data.newFloatValue;
		}

		this.sendSocketData(payload);
	}

	sendSocketData(payload) {
		const readyState = socket.readyState;
		if (readyState === WebSocket.OPEN) {
			socket.send(JSON.stringify(payload))
		} else if (readyState === WebSocket.CONNECTING) {
			console.debug('Socket was still connecting. Ignoring socket send request');
		} else {
			console.info('Socket is in a state of ' + readyState + '. Creating a new socket and ignoring this send request');
			this.connectToSocket();
		}
	}

	addOnConnectedHandler(onConnectedHandler) {
		const newHandlers = this.state.onConnectedHandlers.slice(0);
		newHandlers.push(onConnectedHandler);
		this.setState({ onConnectedHandlers: newHandlers });
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
	REMOTE_PLAY: 'REMOTE_PLAY',
	REVIEW_QUEUE: 'REVIEW_QUEUE',
	CONNECTION_ESTABLISHED: 'CONNECTION_ESTABLISHED'
});
