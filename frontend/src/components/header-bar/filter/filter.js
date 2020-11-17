import React, {useContext, useState, useRef} from 'react';
import {MusicFilterContext} from "../../../services/music-filter-provider";
import {MusicContext} from "../../../services/music-provider";
import {PlaylistContext} from "../../../services/playlist-provider";

export default function Filter() {
	const musicFilterContext = useContext(MusicFilterContext);
	const musicContext = useContext(MusicContext);
	const playlistContext = useContext(PlaylistContext);
	const filterRoot = useRef(null);
	const [filterOpen, setFilterOpen] = useState(false);

	const closeFilter = e => {
		if (!filterRoot.current.contains(e.target)) {
			document.body.removeEventListener('click', closeFilter);
			setFilterOpen(false);
		}
	};

	const handleHiddenChange = () => {
		musicFilterContext.setProviderState({ showHidden: !musicFilterContext.showHidden }, musicContext.reloadTracks);
	};

	const handlePlaylistChange = newPlaylistId => {
		musicContext.setProviderState({ excludedPlaylistId: parseInt(newPlaylistId) }, musicContext.reloadTracks);
	};

	const toggleFilter = () => {
		if (!filterOpen) {
			document.body.addEventListener('click', closeFilter)
		} else {
			document.body.removeEventListener('click', closeFilter)
		}
		setFilterOpen(!filterOpen);
	};

	return (
		<div id="header-filter" ref={filterRoot}>
			<button onClick={toggleFilter}>Filters</button>

			{ filterOpen ?
				<div className="floating-window">
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

					<div>
						<label>
							Exclude songs on Playlist:
							<select
								className="playlist-filter"
								value={musicContext.excludedPlaylistId}
								onChange={e => handlePlaylistChange(e.target.value)}
							>
								<option value={-1}/>
								{ playlistContext.playlists.map(playlist =>
									<option key={playlist.id} value={playlist.id}>
										{ playlist.name }
									</option>
								)}
							</select>
						</label>
					</div>
				</div> : null
			}

		</div>
	)
}

