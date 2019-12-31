import React, {useContext, useState} from 'react';
import {MusicContext} from "../../services/music-provider";
import {MusicFilterContext} from "../../services/music-filter-provider";

export default function SearchBar() {
	const [debounceTimeout, setDebounceTimeout] = useState(null);

	const musicContext = useContext(MusicContext);
	const musicFilterContext = useContext(MusicFilterContext);

	const handleKeyPress = event => {
		if (debounceTimeout) {
			clearTimeout(debounceTimeout);
		}

		if (event === undefined) {
			musicContext.reloadTracks();
		} else if (event.key === 'Enter') {
			event.preventDefault();
			event.stopPropagation();
			musicContext.reloadTracks();
		}
	};

	const debouncedKeyPress = () => {
		if (debounceTimeout) {
			clearTimeout(debounceTimeout);
		}
		const timeout = setTimeout(() => handleKeyPress(), 400);
		setDebounceTimeout(timeout);
	};

	const handleInputChange = event => {
		musicFilterContext.setProviderState({ searchTerm: event.target.value });
		debouncedKeyPress();
	};

	const handleHiddenChange = () => {
		musicFilterContext.setProviderState({ showHidden: !musicFilterContext.showHidden }, musicContext.reloadTracks);
	};

	const clearInput = () => {
		musicFilterContext.setProviderState({ searchTerm: '' }, musicContext.reloadTracks);
	};

	return (
		<div className="d-flex search" onKeyDown={e => e.nativeEvent.propagationStopped = true}>
			<div className="p-relative">
				Search
				<input
					className="search-bar"
					value={musicFilterContext.searchTerm}
					onChange={handleInputChange}
				/>
				{ musicFilterContext.searchTerm
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
