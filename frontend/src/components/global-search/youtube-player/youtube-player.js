import React, {useContext, useEffect, useState} from "react";
import MiniPlayer from "../../playback-controls/mini-player/mini-player";
import {useInterval} from "../../../use-interval";
import {MusicContext} from "../../../services/music-provider";
import {numberWithCommas} from "../../../util";
import YoutubeDlButton from "../../youtube-dl/youtube-dl";
import {formatDateEnglish} from "../../../formatters";

export default function YoutubePlayer(props) {
	const [player, setPlayer] = useState(null);
	const [trackData, setTrackData] = useState({});
	const [playing, setPlaying] = useState(false);
	const [volume, setVolume] = useState(1);
	const [muted, setMuted] = useState(false);
	const [timePlayed, setTimePlayed] = useState(0);

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
		if (player && playing) {
			// noinspection JSDeprecatedSymbols (not actually deprecated)
			setTimePlayed(player.getCurrentTime())
		}
	};

	// Youtube API doesn't provide events we can listen to for updating the time.
	// Instead we have to set an interval to update ourselves
	useInterval(updateTimePlayed, 1000);

	const initVideo = () => {
		// the Player object is created uniquely based on the id in props
		const newPlayer = new window.YT.Player(`youtube-player-${props.video.id}`, {
			videoId: props.video.id,
			events: { onReady: onPlayerReady },
		});

		setPlayer(newPlayer);
	};

	function onPlayerReady(arg) {
		const data = Object.assign({}, trackData);
		data.length = arg.target.getDuration();

		setTrackData(data);
	}

	return <div className="youtube-player">
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
			playing={playing}
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
				if (playing) {
					player.pauseVideo();
				} else {
					player.playVideo();
				}

				setPlaying(!playing);
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
}
