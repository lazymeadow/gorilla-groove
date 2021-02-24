import React, {useRef} from "react";
import {formatTimeFromSeconds} from "../../../formatters";
import {getVolumeIcon} from "../../../util";

export default function MiniPlayer(props) {
	const timeSliderRef = useRef(null);
	const volumeSliderRef = useRef(null);

	if (timeSliderRef.current !== null) {
		if (props.timePlayed === undefined || props.trackData.length === undefined) {
			timeSliderRef.current.value = 0;
		} else {
			timeSliderRef.current.value = props.timePlayed / props.trackData.length;
		}
	}

	if (volumeSliderRef.current !== null) {
		volumeSliderRef.current.value = props.volume;
	}

	const shuffleClass = props.shuffling === undefined ? 'hidden' : '';
	const repeatClass = props.shuffling === undefined ? 'hidden' : '';
	const songChangeClass = props.onPlayNext === undefined ? 'hidden' : '';

	const createNameElement = (name, link) => {
		if (name === undefined) {
			return null;
		} else if (link === undefined) {
			return <li>{name}</li>
		} else {
			return <li>
				<a href={link} target="_blank">{name}</a>
			</li>
		}
	};

	const getPlayButtonClasses = () => {
		if (props.buffering) {
			return 'fa-circle-notch animation-spin'
		} else if (props.playing) {
			return 'fa-pause';
		} else {
			return 'fa-play';
		}
	};

	const albumArtLink = props.trackData.albumArtLink;

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
					style={ albumArtLink
						? { backgroundImage: `url(${albumArtLink})` }
						: {}
					}
				/>
				<div className="song-information">
					<ul>
						{ createNameElement(props.trackData.name, props.nameLink)}
						<li>{props.trackData.artist}</li>
						<li>{props.trackData.album}</li>
						<li>{props.trackData.releaseYear}</li>
					</ul>
					<div className="additional-item">{props.additionalItem}</div>
				</div>
			</div>

			<div className="flex-between bottom-section">
				<div className="playback-controls">
					<i
						onMouseDown={props.onPlayPrevious}
						className={`fas fa-step-backward ${songChangeClass}`}
					/>
					<i
						onMouseDown={props.onPauseChange}
						className={`fas ${getPlayButtonClasses()}`}
					/>
					<i
						onMouseDown={props.onPlayNext}
						className={`fas fa-step-forward ${songChangeClass}`}
					/>
				</div>
				<div className="slider-section">
					<div className="time-slider-wrapper">
						<div className="time-indicators">
							{ formatTimeFromSeconds(props.timePlayed)} / {formatTimeFromSeconds(props.trackData.length) }
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
							className={`fas fa-random control ${shuffleClass} ${props.shuffling ? 'enabled' : ''}`}
						/>
						<i
							onMouseDown={props.onRepeatChange}
							className={`fas fa-sync-alt control ${repeatClass} ${props.repeating ? 'enabled' : ''}`}
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
