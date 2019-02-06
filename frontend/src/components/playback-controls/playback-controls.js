import React from 'react';
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";

export class PlaybackControls extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			currentSessionPlayCounter: 0, // Used to detect when we should play a new song
			lastTime: 0,
			totalTimeListened: 0,
			timeTarget: null,
			listenedTo: false,
			songLink: null
		}
	}

	componentDidMount() {
		let audio = document.getElementById('audio');
		audio.addEventListener('timeupdate', (e) => { this.handleTimeTick(e.target.currentTime) });
		audio.addEventListener('durationchange', (e) => { this.handleDurationChange(e.target.duration) });
		audio.addEventListener('ended', () => { this.context.playNext() });
	}

	componentDidUpdate() {
		// No track to play. Nothing to do
		if (!this.context.playedTrack) {
			return;
		}

		// If our track and time haven't changed, there is nothing to do
		// This breaks some problems with infinite re-rendering we can get into otherwise
		if (this.context.sessionPlayCounter === this.state.currentSessionPlayCounter) {
			return;
		}

		this.handleSongChange();
	}

	// You might think that this could be calculated in handleSongChange() and not need its own function. However,
	// the duration is NOT YET KNOWN when the song changes, because it hasn't fully loaded the metadata. This event
	// triggers some time after the song change, once the metadata itself is loaded
	handleDurationChange(duration) {
		// If someone listens to 60% of a song, we want to mark it as listened to. Keep track of what that target is
		this.setState({ timeTarget: duration * 0.60 })
	}

	handleSongChange() {
		if (this.context.playedTrackIndex == null) {
			return;
		}

		// TODO It's probably actually better to have this fetching happen in the music context
		// so that the album art and the song controls aren't both having to fetch them separate
		Api.get('file/link/' + this.context.playedTrack.id).then((links) => {

			// Start playing the new song
			this.setState({
				currentSessionPlayCounter: this.context.sessionPlayCounter,
				lastTime: 0,
				totalTimeListened: 0,
				listenedTo: false,
				songLink: this.getSongLink(links)
			}, () => {
				let audio = document.getElementById('audio');
				audio.currentTime = 0;
				audio.src = this.getSongLink(links);
				audio.play();
			})
		});
	}

	// noinspection JSMethodCanBeStatic
	getSongLink(links) {
		// TODO swap for S3
		return links.songLink + '?t=' + sessionStorage.getItem('token');
	}

	handleTimeTick(currentTime) {
		let newProperties = { lastTime: currentTime };

		let timeElapsed = currentTime - this.state.lastTime;
		// If the time elapsed went negative, or had a large leap forward (more than 1 second), then it means that someone
		// manually altered the song's progress. Do no other checks or updates
		if (timeElapsed < 0 || timeElapsed > 1) {
			this.setState(newProperties);
			return;
		}

		newProperties.totalTimeListened = this.state.totalTimeListened + timeElapsed;

		if (this.state.timeTarget && newProperties.totalTimeListened > this.state.timeTarget && !this.state.listenedTo) {
			newProperties.listenedTo = true;

			let playedTrack = this.context.playedTrack;
			Api.post('track/mark-listened', { trackId: playedTrack.id })
				.then(() => {
					// Could grab the track data from the backend, but this update is simple to just replicate on the frontend
					playedTrack.playCount++;
					playedTrack.lastPlayed = new Date();

					// We updated the reference rather than dealing with the hassle of updating via setState for multiple collections
					// that we'd have to search and find indexes for. So issue an update to the parent component afterwards
					this.context.forceTrackUpdate();
				})
				.catch((e) => {
					console.error('Failed to update play count');
					console.error(e);
				});
		}

		this.setState(newProperties);
	}

	render() {
		let playedTrack = this.context.playedTrack;
		let src = playedTrack ? this.state.songLink : '';
		return (
			<div>
				Now Playing: {playedTrack ? playedTrack.name : 'Nothing'}
				<div>
					<div>
						<audio id="audio" src={src} controls>
							Your browser is ancient. Be less ancient.
						</audio>
						<button onClick={() => this.context.playPrevious()}>Play Previous</button>
						<button onClick={() => this.context.playNext()}>Play Next</button>
					</div>
					<div>
						<button onClick={() => this.context.setRepeatSongs(!this.context.repeatSongs)}>Repeat</button>
						Repeat is {this.context.repeatSongs ? 'On' : 'Off'}
					</div>
					<div>
						<button onClick={() => this.context.setShuffleSongs(!this.context.shuffleSongs)}>Shuffle</button>
						Shuffle is {this.context.shuffleSongs ? 'On' : 'Off'}
					</div>
				</div>
			</div>
		)
	}
}
PlaybackControls.contextType = MusicContext;
