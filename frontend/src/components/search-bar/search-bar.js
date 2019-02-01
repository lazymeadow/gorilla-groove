import React from 'react';
import {MusicContext} from "../../services/music-provider";

export class SearchBar extends React.Component {
	constructor(props) {
		super(props);
	}

	handleKeyPress(event) {
		if (event.key === 'Enter') {
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
					onChange={(e) => this.context.setSearchTerm(e.target.value)}
					onKeyDown={this.handleKeyPress.bind(this)}
					defaultValue={this.context.searchTerm}
				/>
			</div>
		)
	}
}
SearchBar.contextType = MusicContext;
