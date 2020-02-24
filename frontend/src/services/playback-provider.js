import React from "react";
import * as LocalStorage from "../local-storage";

export const PlaybackContext = React.createContext();

export class PlaybackProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			isPlaying: false,
			isMuted: LocalStorage.getBoolean('muted', 1.0),
			volume: LocalStorage.getNumber('volume', 1.0),

			timePlayedOverride: -1,

			setVolume: (...args) => this.setVolume(...args),
			setMuted: (...args) => this.setMuted(...args),
			clearTimePlayedOverride: (...args) => this.clearTimePlayedOverride(...args),
			setProviderState: (...args) => this.setProviderState(...args)
		}
	}

	setProviderState(state, callback) {
		this.setState(state, callback);
	}

	setMuted(isMuted) {
		this.setState({
			isMuted,
			renderCounter: this.state.renderCounter + 1
		});
		LocalStorage.setBoolean('muted', isMuted);
	}

	setVolume(volume) {
		const floatVolume = parseFloat(volume);
		this.setState({
			volume: floatVolume,
			renderCounter: this.state.renderCounter + 1
		});
		LocalStorage.setNumber('volume', floatVolume);
	}

	clearTimePlayedOverride() {
		this.setState({ timePlayedOverride: 0 });
	}

	render() {
		return (
			<PlaybackContext.Provider value={this.state}>
				{this.props.children}
			</PlaybackContext.Provider>
		)
	}
}
