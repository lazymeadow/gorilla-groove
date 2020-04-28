import React, {useContext, useState} from 'react';
import {MusicContext} from "../../services/music-provider";
import {MusicFilterContext} from "../../services/music-filter-provider";

export default function SearchBar() {
	const musicFilterContext = useContext(MusicFilterContext);
	const musicContext = useContext(MusicContext);

	const [debounceTimeout, setDebounceTimeout] = useState(null);
	const [searchTerm, setSearchTerm] = useState(musicFilterContext.searchTerm);


	const updateSearchTerm = newSearchTerm => {
		if (debounceTimeout) {
			clearTimeout(debounceTimeout);
		}

		musicFilterContext.setProviderState({ searchTerm: newSearchTerm }, musicContext.reloadTracks);
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

	const handleHiddenChange = () => {
		musicFilterContext.setProviderState({ showHidden: !musicFilterContext.showHidden }, musicContext.reloadTracks);
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
				Search
				<input
					className="search-bar"
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

			<div className="hidden-checkbox">
				Show Hidden Tracks
				<i title="Show songs that have been hidden from the library.
This is not the same as private tracks, which are only visible to the owner"
					 className="fas fa-question-circle"/>
				<input
					type="checkbox"
					checked={musicFilterContext.showHidden}
					onChange={handleHiddenChange}
				/>
			</div>
		</div>
	)
}
