import React from "react";
import {isLoggedIn} from "../util";
import {Api} from "../api";

export const SocketContext = React.createContext();

let lastUpdate = -1;

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
		Api.post('currently-listening', { song: null });

		window.addEventListener("beforeunload", this.disconnectSocket.bind(this));
	}

	fetchLatestData() {
		Api.get('currently-listening', { lastUpdate }).then(result => {
			lastUpdate = result.lastUpdate;
			this.setState({ nowListeningUsers: result.currentlyListeningUsers });
			this.fetchLatestData();
		}).catch(() => {
			setTimeout(this.fetchLatestData.bind(this), 2000);
		});
	}

	connectToSocket() {
		// Avoid sending a new connection on logout / login
		// If our last update was not -1 then it means we're already looking for new data
		if (!isLoggedIn() || lastUpdate !== -1) {
			return;
		}

		this.fetchLatestData();
	}

	disconnectSocket() {
		Api.post('currently-listening', { song: null });
	}

	sendPlayEvent(track) {
		if (!track) {
			Api.post('currently-listening', { song: null });
		} else {
			const song = track.private ? 'This track is private' : track.artist + ' - ' + track.name;
			Api.post('currently-listening', { song });
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
