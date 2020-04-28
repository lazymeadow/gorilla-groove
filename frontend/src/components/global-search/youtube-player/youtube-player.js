import React, {useEffect, useRef, useState} from "react";
import MiniPlayer from "../../playback-controls/mini-player/mini-player";
import {useInterval} from "../../../use-interval";
import {numberWithCommas} from "../../../util";
import YoutubeDlButton from "../../youtube-dl/youtube-dl";
import {formatDateEnglish} from "../../../formatters";

const UNSTARTED = -1;
// noinspection JSUnusedLocalSymbols
const ENDED = 0;
const PLAYING = 1;
// noinspection JSUnusedLocalSymbols
const PAUSED = 2;
const BUFFERING = 3;

export default function YoutubePlayer(props) {
	const [player, setPlayer] = useState(null);
	const [trackData, setTrackData] = useState({});
	const [volume, setVolume] = useState(1);
	const [muted, setMuted] = useState(false);
	const [timePlayed, setTimePlayed] = useState(0);
	const [isPlayerReady, setPlayerReady] = useState(false);
	// Can't always rely on YT to tell us if it's buffering. Keep track of our own indicator for first buffer.
	// Maybe they'll fix this in the future. You can repro it by:
	// 1) Having the video be playing
	// 2) Pause the video
	// 3) Seek to a new part of the video that needs to buffer
	// 4) Play the video again, and it will now tell you its state is "buffering" when it means "playing"
	const [finishedBuffering, setFinishedBuffering] = useState(false);

	// Sometimes useEffect() is so annoying. This is actually stated as a viable solution in the
	// React docs. Use a ref to get around useEffect()'s closure binding as the ref can't do stale
	const isPlayingRef = useRef(props.isPlaying);
	const isBufferingRef = useRef(false);
	const [_, setRandom] = useState(0); // Still need a way to force a re-render after we update a ref...
	useEffect(() => { isPlayingRef.current = props.isPlaying; });

	if (Object.keys(trackData).length === 0) {
		const {likes, dislikes} = props.video;
		const likeRatio = likes / (likes + dislikes);
		const likePercent = Math.round(likeRatio * 100);

		setTrackData({
			name: props.video.title,
			artist: props.video.channelTitle,
			album: numberWithCommas(props.video.viewCount) + ` Views (${likePercent}% liked)`,
			releaseYear: formatDateEnglish(props.video.publishedAt),
			albumArtLink: props.video.thumbnails.high.url
		})
	}

	useEffect(() => {
		if (!window.YT) {
			console.error('Youtube Player was rendered before the Youtube API was initialized!')
		} else {
			initVideo();
		}
	}, []);

	const updateTimePlayed = () => {
		if (player && props.isPlaying) {
			// noinspection JSDeprecatedSymbols (not actually deprecated)
			setTimePlayed(player.getCurrentTime())
		}
	};

	// Youtube API doesn't provide events we can listen to for updating the time.
	// Instead we have to set an interval to update ourselves
	useInterval(updateTimePlayed, 1000);

	const onPlayerReady = event => {
		const data = Object.assign({}, trackData);
		data.length = event.target.getDuration();

		event.target.setVolume(100);

		setTrackData(data);
		setPlayerReady(true);
	};

	const onStateChange = newState => {
		// For some reason -1 gets thrown out despite it still buffering
		// It goes, -1, 3, -1, 3, 1
		if (isPlayingRef.current && (newState.data === BUFFERING || newState.data === UNSTARTED)) {
			isBufferingRef.current = true;
			setRandom(Math.random());
		} else {
			// I've seen YT be stupid and insist that a video is buffering indefinitely if you play / pause
			// too many times. So after it's finished buffering once, just say it's done. Not perfect, since
			// someone could just toggle play / pause right away and then lose the ability to see the buffer...
			// but it's not that big of a deal. If only this API was reliable
			if (isBufferingRef.current) {
				setFinishedBuffering(true);
			}
			isBufferingRef.current = false;
			setRandom(Math.random());
		}
	};

	const initVideo = () => {
		// the Player object is created uniquely based on the id in props
		const newPlayer = new window.YT.Player(`youtube-player-${props.video.id}`, {
			videoId: props.video.id,
			events: {
				onReady: onPlayerReady,
				onStateChange: onStateChange
			},
		});

		setPlayer(newPlayer);
	};

	const handleIsPlaying = shouldBePlaying => {
		if (shouldBePlaying) {
			player.playVideo();
		} else {
			player.pauseVideo();
		}
	};

	// We only want one video playing at a time, and the parent component knows which video that is.
	// If we're out of step with the parent component, get ourselves back in sync.
	if (player !== null && player.getPlayerState) {
		const state = player.getPlayerState();
		const isPlaying = state === PLAYING || state === BUFFERING;

		if (props.isPlaying !== isPlaying) {
			handleIsPlaying(props.isPlaying);
		}
	}

	return (
		<div className="youtube-player">
			<iframe
				id={`youtube-player-${props.video.id}`}
				width="0" height="0"
				src={`${props.video.embedUrl}?enablejsapi=1`}
				frameBorder="0"
			/>

			<MiniPlayer
				trackData={trackData}
				nameLink={props.video.videoUrl}
				additionalItem={<YoutubeDlButton video={props.video} iconOverride='fas fa-cloud-upload-alt'/>}
				playing={props.isPlaying}
				buffering={!isPlayerReady || (!finishedBuffering && isBufferingRef.current)}
				timePlayed={timePlayed}
				onTimeChange={(newTimePercent, isHeld) => {
					if (!isHeld) {
						return;
					}

					const newTime = newTimePercent * trackData.length;
					player.seekTo(newTime);
					setTimePlayed(newTime);
				}}
				onPauseChange={() => {
					handleIsPlaying(!props.isPlaying);

					props.setPlayingStatus(props.video.id, !props.isPlaying);
				}}
				volume={volume}
				onVolumeChange={newVolume => {
					// Youtube's API expects an integer between 0 and 100
					player.setVolume(Math.round(newVolume * 100));
					setVolume(newVolume);
				}}
				muted={muted}
				onMuteChange={() => {
					if (muted) {
						player.unMute();
					} else {
						player.mute();
					}

					setMuted(!muted);
				}}
			/>
		</div>
	)
}
