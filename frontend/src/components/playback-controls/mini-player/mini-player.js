import React, {useRef, useState} from "react";
import {formatTimeFromSeconds} from "../../../formatters";
import {getVolumeIcon} from "../../../util";

export default function MiniPlayer(props) {
	const timeSliderRef = useRef(null);
	const volumeSliderRef = useRef(null);

	if (timeSliderRef.current !== null) {
		timeSliderRef.current.value = props.timePlayed / props.trackData.duration;
	}

	if (volumeSliderRef.current !== null) {
		volumeSliderRef.current.value = props.volume;
	}

	// TODO reuse

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
						onMouseDown={props.onPauseChange}
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
							ref={timeSliderRef}
							type="range"
							className="time-slider"
							onMouseUp={e => props.onTimeChange(e.target.value, false)}
							onChange={e => props.onTimeChange(e.target.value, true)}
							min="0"
							max="1"
							step="0.01"
						/>
					</div>
					<div className="bottom-controls">
						<i
							onMouseDown={props.onShuffleChange}
							className={`fas fa-random control ${props.shuffling ? 'enabled' : ''}`}
						/>
						<i
							onMouseDown={props.onRepeatChange}
							className={`fas fa-sync-alt control ${props.repeating ? 'enabled' : ''}`}
						/>

						<i
							className={`fas ${getVolumeIcon(props.volume, props.muted)} volume-icon`}
							onMouseDown={props.onMuteChange}
						/>
						<input
							ref={volumeSliderRef}
							type="range"
							className="volume-slider"
							onMouseUp={e => props.onVolumeChange(e.target.value, false)}
							onChange={e => props.onVolumeChange(e.target.value, true)}
							min="0"
							max="1"
							step="0.01"
						/>
					</div>
				</div>
			</div>
		</div>
	)
}
