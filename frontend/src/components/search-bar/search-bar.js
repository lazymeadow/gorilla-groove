import React, {useContext, useState} from 'react';
import {MusicContext} from "../../services/music-provider";
import {MusicFilterContext} from "../../services/music-filter-provider";
import {CenterView} from "../../enums/site-views";

export default function SearchBar(props) {
	const musicFilterContext = useContext(MusicFilterContext);
	const musicContext = useContext(MusicContext);

	const [debounceTimeout, setDebounceTimeout] = useState(null);
	const [searchTerm, setSearchTerm] = useState(musicFilterContext.searchTerm);


	const updateSearchTerm = newSearchTerm => {
		if (debounceTimeout) {
			clearTimeout(debounceTimeout);
		}

		musicFilterContext.setProviderState({ searchTerm: newSearchTerm }, () => {
			if (props.centerView === CenterView.TRACKS) {
				musicContext.reloadTracks()
			}
		});
	};

	const debouncedKeyPress = newSearchTerm => {
		if (debounceTimeout) {
			clearTimeout(debounceTimeout);
		}
		const timeout = setTimeout(() => updateSearchTerm(newSearchTerm), 400);
		setDebounceTimeout(timeout);
	};

	const handleInputChange = event => {
		setSearchTerm(event.target.value);
		debouncedKeyPress(event.target.value);
	};

	const clearInput = () => {
		setSearchTerm('');
		musicFilterContext.setProviderState({ searchTerm: '' }, musicContext.reloadTracks);
	};

	const handleKeyPress = event => {
		event.nativeEvent.propagationStopped = true;

		if (event.key === 'Enter') {
			event.preventDefault();
			event.stopPropagation();
			updateSearchTerm(event.target.value);
		}
	};

	return (
		<div className="d-flex search" onKeyDown={handleKeyPress}>
			<div className="p-relative">
				<input
					className="search-bar"
					placeholder="Search"
					value={searchTerm}
					onChange={handleInputChange}
				/>
				{ searchTerm
					? <i
						className="fas fa-times-circle close-button"
						onClick={clearInput}
					/>
					: <i/>
				}
			</div>
		</div>
	)
}
