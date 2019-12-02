import React from 'react';
import {MusicContext} from "../../../services/music-provider";

export class ShuffleChaos extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			shuffleChaos: 1
		}
	}

	handleChaosChange(newValue) {
		// Let the value "snap" to the middle if you're close to it
		const adjustedValue = newValue > 0.9 && newValue < 1.1 ? 1 : newValue;
		this.context.setShuffleChaos(adjustedValue);
	}

	render() {
		return (
			<div id="shuffle-chaos" className="display-flex">
				<div className="p-relative">
					<div className="text-center">
					<span title="Shuffle the music, favoring more played or less played songs">
						Play Count Lean <i className="fas fa-question-circle fa-small"/>
					</span>
					</div>

					<div id="middle-line">|</div>

					<div>
						<input
							id="chaos-slider"
							onChange={e => this.handleChaosChange(e.target.value)}
							type="range"
							min="0"
							max="2"
							step="0.01"
							value={this.context.shuffleChaos}
						/>

					</div>

					<div id="slider-label">
						<div>Less</div>
						<div>More</div>
					</div>
				</div>
			</div>
		)
	}
}
ShuffleChaos.contextType = MusicContext;
