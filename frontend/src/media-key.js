let sessionSetUp = false;

export function setupMediaKeySessionIfNeeded(playbackContext, musicContext) {
	if (!('mediaSession' in navigator) || sessionSetUp) {
		return
	}

	navigator.mediaSession.setActionHandler('play', () => {
		playbackContext.setProviderState({ isPlaying: true });
	});
	navigator.mediaSession.setActionHandler('pause', () => {
		playbackContext.setProviderState({ isPlaying: false });
	});
	navigator.mediaSession.setActionHandler('stop', () => {
		playbackContext.setProviderState({ isPlaying: false });
	});

	navigator.mediaSession.setActionHandler('previoustrack', () => {
		musicContext.playPrevious();
	});
	navigator.mediaSession.setActionHandler('nexttrack', () => {
		musicContext.playNext();
	});

	// These could be cool to implement. But I have no idea how to even test them because I do not use
	// a device that even allows interacting with a media session in this way in 2021.

	// navigator.mediaSession.setActionHandler('seekbackward', function() { /* Code excerpted. */ });
	// navigator.mediaSession.setActionHandler('seekforward', function() { /* Code excerpted. */ });
	// navigator.mediaSession.setActionHandler('seekto', function() { /* Code excerpted. */ });

	sessionSetUp = true;
}

export function setMediaKeyTrack(track, artLink) {
	if (!('mediaSession' in navigator)) {
		return
	}

	if (track == null) {
		navigator.mediaSession.metadata = null;
		return;
	}

	navigator.mediaSession.metadata = new MediaMetadata({
		title: track.name,
		artist: track.artist,
		album: track.album,
		artwork: [
			{ src: artLink, sizes: '500x500', type: 'image/png' },
		]
	});
}

export function clearMediaSession() {
	if (!('mediaSession' in navigator)) {
		return
	}

	navigator.mediaSession.metadata = null;
	navigator.mediaSession.setActionHandler('play', null);
	navigator.mediaSession.setActionHandler('pause', null);
	navigator.mediaSession.setActionHandler('stop', null);
	navigator.mediaSession.setActionHandler('previoustrack', null);
	navigator.mediaSession.setActionHandler('nexttrack', null);

	sessionSetUp = false;
}
