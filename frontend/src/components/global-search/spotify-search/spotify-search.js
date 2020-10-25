import React, {useEffect, useState, useContext} from "react";
import {MusicFilterContext} from "../../../services/music-filter-provider";
import {Api} from "../../../api";
import {AlbumArt} from "../../album-art/album-art";
import {LoadingSpinner} from "../../loading-spinner/loading-spinner";
import {toast} from "react-toastify";
import {
	initializeYoutubeApiIfNeeded,
	isYoutubeApiInitialized,
	YoutubeApiVideo, YoutubeVideoState
} from "../../../services/youtube-api-client";

const spotifyIdToYoutubeUrl = {};

export default function SpotifySearch() {
	const [spotifyTracks, setSpotifyTracks] = useState([]);
	const [apiInitialized, setApiInitialized] = useState(isYoutubeApiInitialized());
	const [errorEncountered, setErrorEncountered] = useState(false);
	const [loading, setLoading] = useState(false);
	const [activeVideoSourceId, setActiveVideoSourceId] = useState(null);
	const [activeVideo, setActiveVideo] = useState(null);
	const [activeVideoState, setActiveVideoState] = useState(YoutubeVideoState.UNSTARTED);
	const [pendingDownloads, setPendingDownloads] = useState({});

	const [player, setPlayer] = useState(null);

	const musicFilterContext = useContext(MusicFilterContext);

	useEffect(() => {
		initializeYoutubeApiIfNeeded(() => { setApiInitialized(true) });
	});

	useEffect(() => {
		if (musicFilterContext.searchTerm.length > 0) {
			setLoading(true);
			Api.get('search/spotify/artist/' + musicFilterContext.searchTerm).then(tracks => {
				setSpotifyTracks(tracks);
				setErrorEncountered(false);
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
		} else if (spotifyTracks.length === 0) {
			if (musicFilterContext.searchTerm.length === 0) {
				return <h3 className="text-center">Use the search bar to search for an artist on Spotify!</h3>
			} else if (!loading) {
				return <h3 className="text-center">No results found. Check that you searched for an artist's name</h3>
			}
		}

		return null;
	};

	const getPlayButtonClasses = spotifyTrack => {
		if (spotifyTrack.sourceId !== activeVideoSourceId || activeVideoState === YoutubeVideoState.PAUSED) {
			return 'fa-play';
		}

		if (activeVideoState === YoutubeVideoState.BUFFERING) {
			return 'fa-circle-notch animation-spin'
		} else {
			return 'fa-pause';
		}
	};

	const toggleTrackPlay = spotifyTrack => {
		if (spotifyTrack.sourceId === activeVideoSourceId) {
			if (activeVideoState === YoutubeVideoState.PAUSED) {
				player.playVideo();
				setActiveVideoState(YoutubeVideoState.PLAYING);
			} else {
				player.pauseVideo();
				setActiveVideoState(YoutubeVideoState.PAUSED);
			}
		} else {
			setActiveVideoSourceId(spotifyTrack.sourceId);
			setActiveVideoState(YoutubeVideoState.BUFFERING);
			const term = spotifyTrack.artist + ' ' + spotifyTrack.name;
			Api.get(`search/youtube/term/${term}/length/${spotifyTrack.length}`).then(res => {
				spotifyIdToYoutubeUrl[spotifyTrack.sourceId] = res.videoUrl;

				console.log(res);
				if (res && res.id) {
					setActiveVideo(res);
				} else {
					toast.error('Failed to find a song to play!');
					setActiveVideo(null);
				}
			});
		}
	};

	const onPlayerReady = player => {
		setPlayer(player);
		player.playVideo();
	};

	const onStateChange = newState => {
		if (newState === YoutubeVideoState.PLAYING) {
			setActiveVideoState(newState)
		}
	};

	const importTrack = spotifyTrack => {
		const newDownloads = Object.assign({}, pendingDownloads);
		newDownloads[spotifyTrack.sourceId] = true;
		setPendingDownloads(newDownloads);

		if (spotifyIdToYoutubeUrl[spotifyTrack.sourceId] !== undefined) {
			downloadFromYoutube(spotifyIdToYoutubeUrl[spotifyTrack.sourceId], spotifyTrack)
		} else {
			const term = spotifyTrack.artist + ' ' + spotifyTrack.name;
			Api.get(`search/youtube/term/${term}/length/${spotifyTrack.length}`).then(res => {
				downloadFromYoutube(res.videoUrl, spotifyTrack);
			}).catch(error => {
				console.error(error);
				toast.error(`The download of ${spotifyTrack.name} failed`);

				const newDownloads = Object.assign({}, pendingDownloads);
				newDownloads[spotifyTrack.sourceId] = false;
				setPendingDownloads(newDownloads);
			});
		}
	};

	const downloadFromYoutube = (url, spotifyTrack) => {
		const params = {
			url,
			name: spotifyTrack.name,
			artist: spotifyTrack.artist,
			album: spotifyTrack.album,
			releaseYear: spotifyTrack.releaseYear,
			trackNumber: spotifyTrack.trackNumber,
			artUrl: spotifyTrack.albumArtLink
		};

		Api.post('track/youtube-dl', params).then(track => {
			this.context.addUploadToExistingLibraryView(track);
			toast.success(`${spotifyTrack.name} downloaded successfully`);
		}).catch(error => {
			console.error(error);
			toast.error(`The download of ${spotifyTrack.name} failed`);
		}).finally(() => {
			const newDownloads = Object.assign({}, pendingDownloads);
			newDownloads[spotifyTrack.sourceId] = false;
			setPendingDownloads(newDownloads);
		});
	};

	return (
		<div id="spotify-search">
			<LoadingSpinner visible={loading || (spotifyTracks.length && !apiInitialized)}/>
			{ getDisplayedText() }

			{ apiInitialized && activeVideo !== null ? <YoutubeApiVideo
				videoId={activeVideo.id}
				embedUrl={activeVideo.embedUrl}
				onPlayerReady={onPlayerReady}
				onStateChange={onStateChange}
			/> : null }

			{ spotifyTracks.length > 0 ?
				<table className="song-table full-width">
					<thead>
					<tr>
						<th className="playback-button-column"/>
						<th className="min-width-column"/>
						<th>Name</th>
						<th>Album</th>
						<th className="min-width-column">Year</th>
						<th className="import-column"/>
					</tr>
					</thead>
					<tbody>
					{ spotifyTracks.map(track =>
						<tr key={track.sourceId}>
							<td className="text-center">
								<i className={`fas ${getPlayButtonClasses(track)}`} onClick={() => toggleTrackPlay(track)}/>
							</td>
							<td>
								<AlbumArt artLink={track.albumArtLink}/>
							</td>
							<td>{track.name}</td>
							<td>{track.album}</td>
							<td>{track.releaseYear}</td>
							<td className="text-center">
								{
									pendingDownloads[track.sourceId] === true
										? <span>Importing ...</span>
										: <button onClick={() => importTrack(track)}>Import</button>
								}
							</td>
						</tr>
					)}
					</tbody>
				</table> : null
			}
		</div>
	)
}
