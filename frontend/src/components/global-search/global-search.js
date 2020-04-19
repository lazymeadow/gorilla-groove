import React, {useContext, useEffect, useState} from "react";
import YoutubePlayer from "./youtube-player/youtube-player";
import {Api} from "../../api";
import {MusicFilterContext} from "../../services/music-filter-provider";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {toast} from "react-toastify";

export default function GlobalSearch() {
	const [videos, setVideos] = useState([]);
	const [apiInitialized, setApiInitialized] = useState(!!window.YT);
	const [loading, setLoading] = useState(false);
	const [errorEncountered, setErrorEncountered] = useState(false);

	const musicFilterContext = useContext(MusicFilterContext);

	const initializeYoutubeApi = () => {
		const tag = document.createElement('script');
		tag.src = 'https://www.youtube.com/iframe_api';

		window.onYouTubeIframeAPIReady = () => setApiInitialized(true);

		const firstScriptTag = document.getElementsByTagName('script')[0];
		firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
	};

	useEffect(() => {
		// The way Youtube's custom controls have to work requires injecting a script. It's weird.
		// But we only need to do it once, and only once we visit this page. So check that it isn't already there.
		if (!window.YT) {
			initializeYoutubeApi();
		}

		if (musicFilterContext.searchTerm.length > 0) {
			setLoading(true);
			Api.get('search/youtube/term/' + musicFilterContext.searchTerm).then(res => {
				setVideos(res.videos);
				setLoading(false);
			}).catch(err => {
				console.error(err);
				toast.error("An error was encountered searching YouTube");
				setErrorEncountered(true);
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

	return <div id="global-search d-relative">
		<LoadingSpinner visible={loading || (videos.length && !apiInitialized)}/>
		{ getDisplayedText() }
		<div id="global-search-container">
			{
				apiInitialized
					? videos.map(video => <YoutubePlayer key={video.id} video={video}/>)
					: null
			}
		</div>
	</div>
}
