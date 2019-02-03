import React from 'react';
import {MusicContext} from "../../services/music-provider";

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
			this.context.reloadTracks();
		}
	}

	render() {
		return (
			<div className="search">
				Search
				<input
					className="search-bar"
					onChange={this.handleInputChange.bind(this)}
					onKeyDown={this.handleKeyPress.bind(this)}
					defaultValue={this.context.searchTerm}
				/>
			</div>
		)
	}
}
SearchBar.contextType = MusicContext;
