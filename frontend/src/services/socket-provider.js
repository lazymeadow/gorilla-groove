import React from "react";
import {isLoggedIn} from "../util";
import {Api} from "../api";
import {getCookieValue} from "../cookie";

export const SocketContext = React.createContext();

let socket = null;
let lastUpdate = -1;

let active = false;

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

	fetchLatestData() {
		if (!active) {
			return;
		}

		Api.get('currently-listening', { lastUpdate }).then(result => {
			console.log('Then', result);
			this.fetchLatestData();
		}).catch(result => {
			console.error('Catch', result);
			setTimeout(this.fetchLatestData, 5000);
		});
	}

	connectToSocket() {
		if (!isLoggedIn()) {
			return;
		}

		active = true;

		this.fetchLatestData();

		/*
		const newSocket = new WebSocket(Api.getSocketUri());

		newSocket.onmessage = res => {
			const data = JSON.parse(res.data);
			console.debug('Received socket data', data);

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
	*/
	}

	disconnectSocket() {
		active = false;
	}

	sendPlayEvent(track) {
		const song = track.private ? 'This track is private' : track.artist + ' - ' + track.name;

		Api.post('currently-listening', { song });
	}

	render() {
		return (
			<SocketContext.Provider value={this.state}>
				{this.props.children}
			</SocketContext.Provider>
		)
	}
}
