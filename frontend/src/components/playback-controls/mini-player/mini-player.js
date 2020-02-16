import React from "react";
import {formatTimeFromSeconds} from "../../../formatters";

export default function MiniPlayer(props) {

	// TODO reuse
	const getVolumeIcon = (muted, volume) => {
		if (muted) {
			return 'fa-volume-mute'
		} else if (volume > 0.5) {
			return 'fa-volume-up';
		} else if (volume > 0) {
			return 'fa-volume-down'
		} else {
			return 'fa-volume-off'
		}
	};

	const playPercentage = !isNaN(props.timePlayed / props.trackData.duration)
		? props.timePlayed / props.trackData.duration
		: 0;

	return (
		<div className="song-player mini-player">
			{ props.title !== undefined
				? <div className="title-box text-center">{props.title}</div>
				: null
			}
			<div className="flex-between">
				{/* Use a background image here because it behaves better at staying within boundaries */}
				<div
					className="album-art"
					style={{ backgroundImage: `url(${props.trackData.albumArtLink})` }}
				/>
				<div className="song-information">
					<ul>
						<li>{props.trackData.title}</li>
						<li>{props.trackData.artist}</li>
						<li>{props.trackData.album}</li>
						<li>{props.trackData.releaseYear}</li>
					</ul>
				</div>
			</div>

			<div className="flex-between bottom-section">
				<div className="playback-controls">
					<i
						// onMouseDown={musicContext.playPrevious}
						className="fas fa-step-backward"
					/>
					<i
						// onMouseDown={togglePause}
						className={`fas fa-${props.playing ? 'pause' : 'play'}`}
					/>
					<i
						// onMouseDown={musicContext.playNext}
						className="fas fa-step-forward"
					/>
				</div>
				<div className="slider-section">
					<div className="time-slider-wrapper">
						<div className="time-indicators">
							{formatTimeFromSeconds(props.timePlayed)} / {formatTimeFromSeconds(props.trackData.duration)}
						</div>
						<input
							type="range"
							className="time-slider"
							onChange={() => {}}
							min="0"
							max="1"
							step="0.01"
							value={playPercentage || 0}
						/>
					</div>
					<div className="bottom-controls">
						<i
							// onMouseDown={() => musicContext.setShuffleSongs(!musicContext.shuffleSongs)}
							className={`fas fa-random control ${props.shuffling ? 'enabled' : ''}`}
						/>
						<i
							// onMouseDown={() => musicContext.setRepeatSongs(!musicContext.repeatSongs)}
							className={`fas fa-sync-alt control ${props.repeating ? 'enabled' : ''}`}
						/>

						<i
							className={`fas ${getVolumeIcon()} volume-icon`}
							// onMouseDown={toggleMute}
						/>
						<input
							type="range"
							className="volume-slider"
							onChange={() => {}}
							min="0"
							max="1"
							step="0.01"
							value={props.volume || 1}
						/>
					</div>
				</div>
			</div>
		</div>
	)
}
