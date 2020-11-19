import React, {useEffect} from "react";

export function YoutubeApiVideo(props) {
	if (props.videoId === undefined) {
		throw 'Video ID must be provided to YoutubeApiVideo!';
	}
	if (props.embedUrl === undefined) {
		throw 'Video ID must be provided to YoutubeApiVideo!';
	}

	const initVideo = () => {
		// the Player object is created uniquely based on the id in props
		// noinspection JSUnusedGlobalSymbols
		const player = new window.YT.Player(`youtube-player-${props.videoId}`, {
			videoId: props.videoId,
			events: {
				onReady: (event) => {
					event.target.setVolume(100);
					props.onPlayerReady(player)
				},
				onStateChange: (stateData) => props.onStateChange(stateData.data)
			},
		});
	};

	useEffect(() => {
		if (!window.YT) {
			console.error('Youtube Player was rendered before the Youtube API was initialized!')
		} else {
			initVideo();
		}
	}, []);

	return <iframe
		id={`youtube-player-${props.videoId}`}
		className="d-none"
		width="0"
		height="0"
		src={`${props.embedUrl}?enablejsapi=1`}
		frameBorder="0"
	/>
}

function isYoutubeApiInitialized() {
	// When the script is initialized, this YT property is found on 'window'
	if (window.YT !== undefined) {
		return true;
	}

	// However, if the script fails to initialize for whatever reason (we have seen 429 errors appear)
	// then we need to check if we already included the script. If we did, there's no reason to include it again
	const script = document.getElementById('youtube-iframe-api-script');

	return script !== null;
}

let youtubeApiErrored = undefined;
export function initializeYoutubeApiIfNeeded(onInitializedHandler) {
  // The way Youtube's custom controls have to work requires injecting a script. It's weird.
  // But we only need to do it once, and only once we visit this page. So check that it isn't already there.
	if (isYoutubeApiInitialized()) {
		if (youtubeApiErrored !== undefined) {
			onInitializedHandler(!youtubeApiErrored);
		}
		return;
	}

	const tag = document.createElement('script');
	tag.src = 'https://www.youtube.com/iframe_api';
	tag.id = 'youtube-iframe-api-script';

	window.onYouTubeIframeAPIReady = () => {
		youtubeApiErrored = false;
		onInitializedHandler(true);
	};

	const firstScriptTag = document.getElementsByTagName('script')[0];
	firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

	// We have seen the API fail to initialize, but they offer us no way to detect a failed initialization.
	// So set up a timer just so we don't sit and wait forever.
	setTimeout(() => {
		if (window.YT === undefined) {
			youtubeApiErrored = true;
			onInitializedHandler(false);
		} else {
			youtubeApiErrored = false;
		}
	}, 4000)
}

export const YoutubeVideoState = Object.freeze({
	UNSTARTED: -1,
	ENDED: 0,
	PLAYING: 1,
	PAUSED: 2,
	BUFFERING: 3
});


