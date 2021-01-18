import React, {useContext, useEffect, useState} from 'react';
import {TrackList} from "..";
import {NowPlayingList} from "../now-playing-list/now-playing-list";
import {AlbumArt} from "../album-art/album-art";
import TrackSourceList from "../track-source-list/track-source-list";
import HeaderBar from "../header-bar/header-bar";
import {MusicContext} from "../../services/music-provider";
import {SiteStats} from "../site-stats/site-stats";
import {isLoggedIn} from "../../util";
import {notifyVersion} from "../../services/version";
import {SocketContext} from "../../services/socket-provider";
import PlaybackControls from "../playback-controls/playback-controls";
import {UserContext} from "../../services/user-provider";
import {PlaylistContext} from "../../services/playlist-provider";
import {CenterView} from "../../enums/site-views";
import RemotePlayManagement from "../remote-play/management/remote-play-management";
import {DeviceContext} from "../../services/device-provider";
import GlobalSearch from "../global-search/global-search";
import ReviewQueue from "../review-queue/review-queue";
import {ReviewQueueContext} from "../../services/review-queue-provider";
import SpotifySearch from "../global-search/spotify-search/spotify-search";
import BackgroundTaskProgress from "../background-task-progress/background-task-progress";

export default function SiteLayout(props) {
	const [centerView, setCenterView] = useState(CenterView.TRACKS);

	if (!isLoggedIn()) {
		console.info('Not logged in. Awaiting redirect');
		props.history.push('/login'); // Redirect to the login page now that we logged out

		return <div/>;
	}

	// Is it possible to call functions on these contexts without being affected by their re-renders?
	// All this view does is initialize state. It doesn't need to be re-rendered by the result.
	// If this is possible it'll cut down on the site-wide re-renders
	const musicContext = useContext(MusicContext);
	const socketContext = useContext(SocketContext);
	const userContext = useContext(UserContext);
	const deviceContext = useContext(DeviceContext);
	const playlistContext = useContext(PlaylistContext);
	const reviewQueueContext = useContext(ReviewQueueContext);

	useEffect(() => {
		userContext.initialize().catch(error => {
			console.error(error);
			// If we had an error here it PROBABLY means we had a failure to login
			props.history.push('/login');
		});
		musicContext.loadSongsForUser();
		playlistContext.loadPlaylists();
		reviewQueueContext.fetchReviewQueueSources();
		deviceContext.loadOtherDevices();

		socketContext.connectToSocket();

		// After we tell the server about our device load server side information about it
		notifyVersion().then(deviceContext.loadOwnDevice);
	}, []);

	const displayedColumns = musicContext.columnPreferences
		.filter(columnPreference => columnPreference.enabled)
		.map(columnPreference => columnPreference.name);

	const getCenterView = centerView => {
		if (centerView === CenterView.REMOTE_DEVICES) {
			return <RemotePlayManagement/>
		} else if (centerView === CenterView.GLOBAL_SEARCH) {
			return <GlobalSearch/>
		} else if (centerView === CenterView.REVIEW_QUEUE) {
			return <ReviewQueue/>
		} else if (centerView === CenterView.SPOTIFY_SEARCH) {
			return <SpotifySearch/>
		}
	};

	return (
		<div className="full-screen border-layout">
			{ deviceContext.isInPartyMode() ? <div id="party-border" className="animation-rainbow-border"/> : null }

			<div className="border-layout-north">
				<HeaderBar
					centerView={centerView}
				/>
			</div>
			<div className="border-layout-west">
				<TrackSourceList
					playlists={playlistContext.playlists}
					centerView={centerView}
					setCenterView={setCenterView}
				/>
			</div>

			<div id="center-view" className="border-layout-center track-list-container p-relative">
				{
					centerView === CenterView.TRACKS
						? <TrackList
							columns={displayedColumns}
							userTracks={musicContext.viewedTracks}
							trackView={true}
						/>
						: getCenterView(centerView)
				}
			</div>

			<div className="border-layout-east track-list-container">
				<NowPlayingList
					columns={["#", "Name"]}
					userTracks={musicContext.nowPlayingTracks}
					trackView={false}
				/>
			</div>
			<div className="border-layout-southwest">
				<AlbumArt artLink={musicContext.playedAlbumArtUrl}/>
			</div>
			<div className="border-layout-south">
				<div className="p-relative flex-between full-height">
					<PlaybackControls/>
					<BackgroundTaskProgress/>
				</div>
			</div>
			<div className="border-layout-southeast">
				<SiteStats/>
			</div>
		</div>
	)
}
