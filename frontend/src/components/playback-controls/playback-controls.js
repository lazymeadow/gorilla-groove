import React from 'react';
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";

export class PlaybackControls extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			playedTrackId: null,
			lastTime: 0,
			totalTimeListened: 0,
			timeTarget: null,
			listenedTo: false
		}
	}

	componentDidMount() {
		let audio = document.getElementById('audio');
		audio.addEventListener('timeupdate', (e) => { this.handleTimeTick(e.target.currentTime) });
		audio.addEventListener('durationchange', (e) => { this.handleDurationChange(e.target.duration) });
		audio.addEventListener('ended', (e) => { this.handleSongEnd() });
	}

	componentDidUpdate() {
		if (!this.state.playedTrackId || this.context.playedTrack.id !== this.state.playedTrackId) {
			this.handleSongChange();
		}
	}

	// You might think that this could be calculated in handleSongChange() and not need its own function. However,
	// the duration is NOT YET KNOWN when the song changes, because it hasn't fully loaded the metadata. This event
	// triggers some time after the song change, once the metadata itself is loaded
	handleDurationChange(duration) {
		// If someone listens to 60% of a song, we want to mark it as listened to. Keep track of what that target is
		this.setState({ timeTarget: duration * 0.60 })
	}

	handleSongChange() {
		// Start playing the new song
		if (this.context.playedTrackIndex != null) {
			let audio = document.getElementById('audio');
			audio.play();

			this.setState({
				playedTrackId: this.context.playedTrack.id,
				lastTime: 0,
				totalTimeListened: 0,
				listenedTo: false
			})
		}
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

	handleSongEnd() {
		if (this.context.shuffleSongs) {
			// If we're shuffling and have more songs to shuffle through, play a random song
			if (this.context.songIndexesToShuffle.length > 0) {
				this.context.playIndex(this.getRandomIndex())

				// If we are out of songs to shuffle through, but ARE repeating, reset the shuffle list and pick a random one
			} else if (this.context.repeatSongs) {
				this.context.resetShuffleIndexes();
				this.context.playIndex(this.getRandomIndex())
			}
		} else {
			// If we aren't shuffling, and we have more songs, just pick the next one
			if (this.context.playedTrackIndex + 1 < this.context.nowPlayingTracks.length) {
				this.context.playIndex(this.context.playedTrackIndex + 1);

				// Otherwise, if we have run out of songs, but are repeating, start back over from 0
			} else if (this.context.repeatSongs) {
				this.context.playIndex(0);
			}
		}
	}

	getRandomIndex() {
		let shuffleIndexes = this.context.songIndexesToShuffle;
		return shuffleIndexes[Math.floor(Math.random() * shuffleIndexes.length)];
	}

	render() {
		let playedTrack = this.context.playedTrack;
		let src = playedTrack ? Api.getSongResourceLink(playedTrack.fileName) : '';
		return (
			<div>
				Now Playing: {playedTrack ? playedTrack.name : 'Nothing'}
				<div>
					<div>
						<audio id="audio" src={src} controls>
							Your browser is ancient. Be less ancient.
						</audio>
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
