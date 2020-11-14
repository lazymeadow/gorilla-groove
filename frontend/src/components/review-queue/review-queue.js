import React, {useContext, useEffect, useState} from "react";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {toast} from "react-toastify";
import {PlaybackContext} from "../../services/playback-provider";
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";
import {ReviewQueueContext} from "../../services/review-queue-provider";
import {ReviewSourceType} from "./review-queue-management/review-queue-management";

let lastReviewSourceId = undefined;

export default function ReviewQueue() {
	const [reviewTrack, setReviewTrack] = useState(null);
	const [trackLinks, setTrackLinks] = useState({});
	const [loading, setLoading] = useState(true);

	const musicContext = useContext(MusicContext);
	const reviewQueueContext = useContext(ReviewQueueContext);
	const playbackContext = useContext(PlaybackContext);

	// Pass nextTrack in as a param, as we can't check "reviewTrack" due to React setState lag
	const fetchReviewTracks = nextTrack => {
		lastReviewSourceId = reviewQueueContext.viewedReviewSourceId;

		return reviewQueueContext.fetchReviewTracks(reviewQueueContext.viewedReviewSourceId).then(tracks => {
			if (tracks.length === 0) {
				setReviewTrack(null);
				setLoading(false);
				return null;
			}

			if (reviewTrack === null || nextTrack === undefined || tracks[0].id !== nextTrack.id) {
				setReviewTrack(tracks[0]);
				fetchLinksForTrack(tracks[0]);
			}

			return tracks[0]
		})
	};

	const fetchLinksForTrack = track => {
		if (track != null) {
			Api.get('file/link/' + track.id).then(links => {
				setTrackLinks(links);
				setLoading(false);
			});
		} else {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchReviewTracks();
	}, []);

	const loadNextTrack = () => {
		let nextTrack = undefined;
		if (reviewQueueContext.reviewQueueTracks.length > 1) {
			nextTrack = reviewQueueContext.reviewQueueTracks[1];

			setReviewTrack(nextTrack);
			fetchLinksForTrack(nextTrack);

			// FIXME This is inefficient, since this is also going to request its own links. But I don't want
			// to deal with making this more optimal right now
			if (playbackContext.isPlaying) {
				musicContext.playTracks([nextTrack]);
			}
		}

		// Fetch the sources, just to make sure we keep up-to-date counts on all the queues
		reviewQueueContext.fetchReviewQueueSources();

		fetchReviewTracks(nextTrack);
	};

	const playSong = () => {
		musicContext.playTracks([reviewTrack]);
	};

	const reviewUp = () => {
		Api.post(`review-queue/track/${reviewTrack.id}/approve`).then(() => {
			toast.success('Track added to your library');
			loadNextTrack()
		});
	};

	const reviewDown = () => {
		Api.delete(`review-queue/track/${reviewTrack.id}`).then(() => {
			toast.success('Track rejected successfully');
			loadNextTrack()
		});
	};

	const reviewSkip = () => {
		Api.post(`review-queue/track/${reviewTrack.id}/skip`).then(() => {
			toast.success('Track moved to the back of your review queue');
			loadNextTrack()
		});
	};

	const getDisplayName = () => {
		if (reviewTrack.artist) {
			return reviewTrack.name + ' - ' + reviewTrack.artist;
		} else {
			return reviewTrack.name;
		}
	};

	const getSourceDescription = () => {
		const source = reviewQueueContext.reviewQueueSourceIdToSource[reviewTrack.reviewSourceId];
		switch (source.sourceType) {
			case ReviewSourceType.ARTIST:
				return 'New release by artist: ' + source.displayName;
			case ReviewSourceType.YOUTUBE_CHANNEL:
				return 'From YouTube channel: ' + source.displayName;
			case ReviewSourceType.USER_RECOMMEND:
				return 'Recommended by user: ' + source.displayName;
			default:
				throw 'Unknown review source!'
		}
	};

	if (reviewQueueContext.viewedReviewSourceId !== lastReviewSourceId) {
		fetchReviewTracks();
	}

	return <div id="review-queue" className="p-relative text-center full-height">
		<LoadingSpinner visible={loading}/>
		{
			reviewTrack !== null ? (
				<div>
					<img id="review-album-art" src={trackLinks.albumArtLink}/>
					<div>{getDisplayName(reviewTrack)}</div>
					<div className="small-text">{getSourceDescription()}</div>
					<div className="review-buttons">
						<i className="fa fa-thumbs-down" title="Delete from your review queue" onClick={reviewDown}/>
						<i className="fa fa-redo" title="Skip and go to the next" onClick={reviewSkip}/>
						<i className="fa fa-thumbs-up" title="Add to your library" onClick={reviewUp}/>
					</div>
					<div>
						<button onClick={() => playSong()}>Start Reviewing</button>
					</div>
				</div>
			) : (
				!loading ? <div>You have no more tracks to review! Good job.</div> : null
			)
		}
	</div>
}
