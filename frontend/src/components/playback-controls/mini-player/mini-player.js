import React from "react";

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

	return (
		<div className="song-player mini-player">
			<div className="flex-between">
				{/* Use a background image here because it behaves better at staying within boundaries */}
				<div
					className="album-art"
					style={{ backgroundImage: `url(${props.albumLink})` }}
				/>
				<div className="song-information">
					<ul>
						<li>Leviathan</li>
						<li>Alestorm</li>
						<li>Black Sails At Midnight</li>
						<li>2009</li>
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
						<div className="time-indicators">0:00 / 5:01</div>
						<input
							type="range"
							className="time-slider"
							// onChange={changePlayTime}
							min="0"
							max="1"
							step="0.01"
							value={props.currentTime}
						/>
					</div>
					<div className="bottom-controls">
						<i
							// onMouseDown={() => musicContext.setShuffleSongs(!musicContext.shuffleSongs)}
							className={`fas fa-random control ${props.shuffleSongs ? 'enabled' : ''}`}
						/>
						<i
							// onMouseDown={() => musicContext.setRepeatSongs(!musicContext.repeatSongs)}
							className={`fas fa-sync-alt control ${props.repeatSongs ? 'enabled' : ''}`}
						/>

						<i
							className={`fas ${getVolumeIcon()} volume-icon`}
							// onMouseDown={toggleMute}
						/>
						<input
							type="range"
							className="volume-slider"
							// onChange={changeVolume}
							min="0"
							max="1"
							step="0.01"
							// value={volume}
						/>
					</div>
				</div>
			</div>
		</div>
	)
}
