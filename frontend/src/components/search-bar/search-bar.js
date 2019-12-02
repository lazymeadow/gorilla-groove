import React from 'react';
import {MusicContext} from "../../services/music-provider";
import * as ReactDOM from "react-dom";

export class SearchBar extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			debounceTimeout: null,
		}
	}

	handleInputChange(event) {
		this.context.setSearchTerm(event.target.value);
		this.debouncedKeyPress();
	}

	debouncedKeyPress() {
		if (this.state.debounceTimeout) {
			clearTimeout(this.state.debounceTimeout);
		}
		let timeout = setTimeout(() => this.handleKeyPress(), 400);
		this.setState({ debounceTimeout: timeout });
	}

	handleKeyPress(event) {
		if (this.state.debounceTimeout) {
			clearTimeout(this.state.debounceTimeout);
		}

		if (event === undefined) {
			this.context.reloadTracks();
		} else if (event.key === 'Enter') {
			event.preventDefault();
			event.stopPropagation();
			this.context.reloadTracks();
		}
	}

	componentDidMount() {
		// I'm writing this comment in a mild anger, but react's event system really seems to be pretty bad,
		// at least for what I want to do.
		// The main track list attaches a keydown listener so it can listen for 'enter' and play songs when
		// 'enter' is pushed. In order to do this, we need to use 'addEventListener()', which does not go
		// along with React's flow of events. It is a native event.
		// Unfortunately, this now means that using any of React's built in helpers will no longer work
		// because we need to supersede track list listener and prevent it from firing
		// Unfortunately, there does not seem to be any way for a React event to stop a native event,
		// because the native events happen FIRST. So for anything dealing with keydowns, we have to
		// use native events instead or React ones
		ReactDOM.findDOMNode(this).addEventListener('keydown', e => { this.handleKeyPress(e) });
	}

	componentWillUnmount() {
		ReactDOM.findDOMNode(this).removeEventListener('keydown', e => { this.handleKeyPress(e) });
	}

	clearInput() {
		this.context.setSearchTerm('', this.context.reloadTracks);
	}

	render() {
		return (
			<div className="search">
				Search
				<input
					className="search-bar"
					value={this.context.searchTerm}
					onChange={this.handleInputChange.bind(this)}
				/>
				{ this.context.searchTerm
					? <i
						className="fas fa-times-circle close-button"
						onClick={this.clearInput.bind(this)}
					/>
					: <i/>
				}
			</div>
		)
	}
}
SearchBar.contextType = MusicContext;
