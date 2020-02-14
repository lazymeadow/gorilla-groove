import React from "react";
import {isLoggedIn} from "../util";
import {Api} from "../api";
import {getDeviceId} from "./version";
import {RemotePlayType} from "../components/remote-play/remote-play-type";

export const SocketContext = React.createContext();

let lastEventId = -1;

export class SocketProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			nowListeningUsers: {},

			connectToSocket: (...args) => this.connectToSocket(...args),
			sendPlayEvent: (...args) => this.sendPlayEvent(...args)
		}
	}

	componentDidMount() {
		// Clear the song immediately if someone refreshed their browser
		Api.post('event/NOW_PLAYING', {
			trackId: null,
			deviceId: getDeviceId()
		});

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
		if (message.remotePlayAction === RemotePlayType.PLAY_SET_SONGS) {
			this.props.musicContext.playTracks(message.tracks)
		} else if (message.remotePlayAction === RemotePlayType.ADD_SONGS_NEXT) {
			this.props.musicContext.playTracksNext(message.tracks)
		} else if (message.remotePlayAction === RemotePlayType.ADD_SONGS_LAST) {
			this.props.musicContext.playTracksLast(message.tracks)
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
		Api.post('event/NOW_PLAYING', {
			trackId: null,
			deviceId: getDeviceId()
		});
	}

	sendPlayEvent(track) {
		Api.post('event/NOW_PLAYING', {
			trackId: track ? track.id : null,
			deviceId: getDeviceId()
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
