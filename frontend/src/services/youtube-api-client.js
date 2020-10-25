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
		width="0" height="0"
		src={`${props.embedUrl}?enablejsapi=1`}
		frameBorder="0"
	/>
}

export function isYoutubeApiInitialized() {
	return !!window.YT;
}

export function initializeYoutubeApiIfNeeded(onInitializedHandler) {
  // The way Youtube's custom controls have to work requires injecting a script. It's weird.
  // But we only need to do it once, and only once we visit this page. So check that it isn't already there.
	if (isYoutubeApiInitialized()) {
		return;
	}

	const tag = document.createElement('script');
	tag.src = 'https://www.youtube.com/iframe_api';

	window.onYouTubeIframeAPIReady = onInitializedHandler;

	const firstScriptTag = document.getElementsByTagName('script')[0];
	firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
}

export const YoutubeVideoState = Object.freeze({
	UNSTARTED: -1,
	ENDED: 0,
	PLAYING: 1,
	PAUSED: 2,
	BUFFERING: 3
});


