import React, {useContext, useEffect, useState} from "react";
import YoutubePlayer from "./youtube-player/youtube-player";
import {Api} from "../../api";
import {MusicFilterContext} from "../../services/music-filter-provider";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {toast} from "react-toastify";
import {PlaybackContext} from "../../services/playback-provider";
import {initializeYoutubeApiIfNeeded, isYoutubeApiInitialized} from "../../services/youtube-api-client";

export default function GlobalSearch() {
	const [videos, setVideos] = useState([]);
	const [playingId, setPlayingId] = useState(null);
	const [apiInitialized, setApiInitialized] = useState(isYoutubeApiInitialized());
	const [loading, setLoading] = useState(false);
	const [errorEncountered, setErrorEncountered] = useState(false);

	const musicFilterContext = useContext(MusicFilterContext);
	const playbackContext = useContext(PlaybackContext);

	useEffect(() => {
		initializeYoutubeApiIfNeeded(() => { setApiInitialized(true) });

		if (musicFilterContext.searchTerm.length > 0) {
			setLoading(true);
			Api.get('search/youtube/term/' + musicFilterContext.searchTerm).then(res => {
				setVideos(res.videos);
			}).catch(err => {
				console.error(err);
				toast.error("An error was encountered searching YouTube");
				setErrorEncountered(true);
			}).finally(() => {
				setLoading(false);
			});
		}
	}, [musicFilterContext.searchTerm]);

	const getDisplayedText = () => {
		if (errorEncountered) {
			return <h3 className="text-center">Oh no! Something went wrong :(</h3>
		} else if (videos.length === 0) {
			if (musicFilterContext.searchTerm.length === 0) {
					return <h3 className="text-center">Use the search bar to find videos!</h3>
			} else if (!loading) {
					return <h3 className="text-center">No results found</h3>
			}
		}

		return null;
	};

	const setPlayingStatus = (videoId, nowPlaying) => {
		// Handle pausing the "main" song player. Start playing the YT video after it finishes to avoid race conditions
		if (nowPlaying && playbackContext.isPlaying) {
			playbackContext.setProviderState(
				{ isPlaying: false },
				() => setPlayingId(videoId)
			);
		} else {
			setPlayingId(nowPlaying ? videoId : null);
		}
	};

	// Handle the "main" song player being started with a YT video playing
	if (playbackContext.isPlaying && playingId !== null) {
		setPlayingId(null);
	}

	return <div id="global-search">
		<LoadingSpinner visible={loading || (videos.length && !apiInitialized)}/>
		{ getDisplayedText() }
		<div id="global-search-container">
			{
				apiInitialized
					? videos.map(video => <YoutubePlayer
						key={video.id}
						video={video}
						isPlaying={video.id === playingId}
						setPlayingStatus={setPlayingStatus}
					/>)
					: null
			}
		</div>
	</div>
}
