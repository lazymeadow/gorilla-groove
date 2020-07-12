import React, {useContext, useEffect, useState} from "react";
import {MusicFilterContext} from "../../services/music-filter-provider";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {toast} from "react-toastify";
import {PlaybackContext} from "../../services/playback-provider";
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";

export default function ReviewQueue() {
	const [queuedTracks, setQueuedTracks] = useState([]);
	const [reviewTrack, setReviewTrack] = useState(null);
	const [trackLinks, setTrackLinks] = useState({});
	const [loading, setLoading] = useState(true);
	const [tracksToReview, setTracksToReview] = useState(null);
	const [totalTracksToReview, setTotalTracksToReview] = useState(null);

	const musicContext = useContext(MusicContext);
	const musicFilterContext = useContext(MusicFilterContext);
	const playbackContext = useContext(PlaybackContext);

	const fetchReviewTracks = () => {
		return Api.get('review-queue').then(res => {
			const tracks = res.content;

			setTracksToReview(tracks);
			setTotalTracksToReview(res.numberOfElements);
			if (tracks.length === 0) {
				setLoading(false);
				setReviewTrack(null);
				return null;
			}

			setReviewTrack(tracks[0]);
			setQueuedTracks(tracks.slice(1));

			return tracks[0]
		})
	};

	const fetchLinksForTrack = track => {
		if (track != null) {
			Api.get('file/link/' + track.id).then(links => {
				setTrackLinks(links);
				setLoading(false);
			});
		}
	};

	useEffect(() => {
		fetchReviewTracks().then(fetchLinksForTrack)
	}, []);

	const loadNextTrack = () => {
		if (tracksToReview.length > 1) {
			const nextTrack = tracksToReview[1];
			setReviewTrack(nextTrack);
			fetchLinksForTrack(nextTrack);
			
			// FIXME This is inefficient, since this is also going to request its own links. But I don't want
			// to deal with making this more optimal right now
			if (playbackContext.isPlaying) {
				musicContext.playTracks([nextTrack]);
			}

			const newTracks = tracksToReview.splice(0);
			newTracks.shift(); // Drop the track we just skipped. It'll get appropriately re-added on the end when our request finishes
			setTracksToReview(newTracks);
		}

		fetchReviewTracks();
	};

	const playSong = () => {
		musicContext.playTracks([reviewTrack]);
	};

	const reviewUp = () => {
		Api.post(`review-queue/track/${reviewTrack.id}/approve`).then(loadNextTrack)
	};

	const reviewDown = () => {
		Api.delete(`review-queue/track/${reviewTrack.id}`).then(loadNextTrack);
	};

	const reviewSkip = () => {
		Api.post(`review-queue/track/${reviewTrack.id}/skip`).then(loadNextTrack)
	};

	return <div id="review-queue" className="d-relative text-center">
		<LoadingSpinner visible={false}/>
		{
			reviewTrack !== null ? (
				<div>
					<img id="review-album-art" src={trackLinks.albumArtLink}/>
					<div>{reviewTrack.name} - {reviewTrack.artist}</div>
					<div>{ totalTracksToReview } total track(s) to review</div>
					<div className="review-buttons">
						<i className="fa fa-thumbs-up" title="Add to your library" onClick={reviewUp}/>
						<i className="fa fa-redo" title="Skip and go to the next" onClick={reviewSkip}/>
						<i className="fa fa-thumbs-down" title="Delete from your review queue" onClick={reviewDown}/>
					</div>
					<div>
						<button onClick={() => playSong()}>Start Reviewing</button>
					</div>
				</div>
			) : (
				<div>
					You have no more tracks to review! Good job.
				</div>
			)
		}
	</div>
}
