import React from "react";
import {isLoggedIn} from "../util";
import {Api} from "../api";
import {getCookieValue} from "../cookie";

export const SocketContext = React.createContext();

let socket = null;

export class SocketProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			nowListeningUsers: {},

			connectToSocket: (...args) => this.connectToSocket(...args),
			disconnectSocket: (...args) => this.disconnectSocket(...args),
			sendPlayEvent: (...args) => this.sendPlayEvent(...args)
		}
	}

	connectToSocket() {
		if (!isLoggedIn()) {
			return;
		}

		const newSocket = new WebSocket(Api.getSocketUri());

		newSocket.onmessage = res => {
			const data = JSON.parse(res.data);

			const email = data.userEmail;
			delete data.userEmail;

			const newNowListeningUsers = Object.assign({}, this.state.nowListeningUsers);
			newNowListeningUsers[email] = data;

			this.setState({ nowListeningUsers: newNowListeningUsers })
		};
		newSocket.onclose = () => {
			console.debug('WebSocket was closed. Reconnecting');
			this.connectToSocket();
		};

		socket = newSocket;
		this.setState({
			nowListeningUsers: {}
		});
	}

	disconnectSocket() {
		if (socket) {
			socket.close();
		}
	}

	sendPlayEvent(track) {
		if (!socket) {
			return;
		}

		const payload = {
			userEmail: getCookieValue('loggedInEmail')
		};

		// Had a server-side deserialization error once saying this was missing... Not sure how
		if (!payload.userEmail) {
			console.error('No user email found!? Not sending socket message');
			return;
		}

		if (track) {
			payload.trackId = track.id;
			payload.trackArtist = track.private ? 'This track' : track.artist;
			payload.trackName = track.private ? 'is private' : track.name;
		}

		console.debug('About to send socket data', new Date());
		console.debug(payload);
		const readyState = socket.readyState;

		if (readyState === WebSocket.OPEN) {
			socket.send(JSON.stringify(payload))
		} else if (readyState === WebSocket.CONNECTING) {
			console.info('Socket was still connecting. Ignoring socket send request');
		} else {
			console.info('Socket is in a state of ' + readyState + '. Creating a new socket and ignoring this send request');
			this.connectToSocket();
		}
	}

	render() {
		return (
			<SocketContext.Provider value={this.state}>
				{this.props.children}
			</SocketContext.Provider>
		)
	}
}
