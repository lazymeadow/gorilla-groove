import React, {useContext, useEffect, useState} from "react";
import {MusicFilterContext} from "../../services/music-filter-provider";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {toast} from "react-toastify";
import {PlaybackContext} from "../../services/playback-provider";
import {Api} from "../../api";

export default function ReviewQueue() {
	const [queuedTracks, setQueuedTracks] = useState([]);
	const [reviewTrack, setReviewTrack] = useState(null);
	const [trackLinks, setTrackLinks] = useState({});
	const [loading, setLoading] = useState(true);

	const musicFilterContext = useContext(MusicFilterContext);
	const playbackContext = useContext(PlaybackContext);

	useEffect(() => {
		Api.get('review-queue').then(res => {
			const tracks = res.content;
			if (tracks.length === 0) {
				setLoading(false);
				return;
			}

			setReviewTrack(tracks[0]);
			setQueuedTracks(tracks.slice(1));

			Api.get('file/link/' + tracks[0].id).then(links => {
				console.log(tracks[0], links);
				setTrackLinks(links);
				setLoading(false);
			});
		})
	}, []);

	return <div id="review-queue" className="d-relative text-center">
		<LoadingSpinner visible={false}/>
		{
			reviewTrack !== null ? (
				<div>
					<img id="review-album-art" src={trackLinks.albumArtLink}/>
					<div>{reviewTrack.name} - {reviewTrack.artist}</div>
					<div className="review-buttons">
						<i className="fa fa-thumbs-up"/>
						<i className="fa fa-redo"/>
						<i className="fa fa-thumbs-down"/>
					</div>
					<div>
						<button>Start Reviewing</button>
					</div>
				</div>
			) : null
		}
	</div>
}
