import React from 'react';
import {Api} from "../../api";

export class PlaybackControls extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
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
	}

	componentDidUpdate(nextProps) {
		if (nextProps.playedTrackIndex !== this.props.playedTrackIndex) {
			this.handleSongChange();
		}
	}

	// You might think that this could be calculated in handleSongChange() and not need its own function. However,
	// the duration is NOT YET KNOWN when the song changes, because it hasn't fully loaded the metadata. This event
	// triggers some time after the song change, once the metadata itself is loaded
	handleDurationChange(duration) {
		// If someone listens to 51% of a song, we want to mark it as listened to. Keep track of what that target is
		this.setState({ timeTarget: duration * 0.51 })
	}

	handleSongChange() {
		console.log("Handle song change");
		// Start playing the new song
		if (this.props.playedTrackIndex != null) {
			let audio = document.getElementById('audio');
			audio.play();

			this.setState({
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
			console.log("Time was adjusted. Skipping");
			this.setState(newProperties);
			return;
		}

		newProperties.totalTimeListened = this.state.totalTimeListened + timeElapsed;

		if (this.state.timeTarget && newProperties.totalTimeListened > this.state.timeTarget && !this.state.listenedTo) {
			// TODO update time listened to on the backend
			console.log("Song was listened to");
			newProperties.listenedTo = true;
		}

		this.setState(newProperties);
	}

	render() {
		let playedTrack = this.props.playedTrackIndex != null ? this.props.nowPlayingTracks[this.props.playedTrackIndex] : null;
		let src = playedTrack ? Api.getSongResourceLink(playedTrack.track.fileName) : '';
		return (
			<div>
				Now Playing: {playedTrack ? playedTrack.track.name : 'Nothing'}
				<div>
					<button>Start</button>
					<button>Stop</button>
					<audio id="audio" src={src} controls>
						Your browser is ancient. Be less ancient.
					</audio>
				</div>
			</div>
		)
	}
}
