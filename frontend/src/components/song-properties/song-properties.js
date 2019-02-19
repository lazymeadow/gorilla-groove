import React from 'react';
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";
import {Modal} from "../modal/modal";

let defaultImageLink = './images/unknown-art.jpg';

export class SongProperties extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			modalOpen: false
		}
	}

	componentDidUpdate() {

	}

	setModalOpen(isOpen) {
		this.setState({ modalOpen: isOpen })
	}

	render() {
		return (
			<div onClick={() => this.setModalOpen(true)}>
				Properties
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setModalOpen(false)}
				>
					<div id="song-properties" className="form-modal">

						<h2>Track Properties</h2>

						<div className="flex-label">
							<label htmlFor="property-name">Name</label>
							<input id="property-name" name="property-name" className="long-property" type="text"/>
						</div>

						<div className="flex-label">
							<label htmlFor="property-artist">Artist</label>
							<input id="property-artist" name="property-artist" className="long-property" type="text"/>
						</div>

						<div className="flex-label">
							<label htmlFor="property-featuring">Featuring</label>
							<input id="property-featuring" name="property-featuring" className="long-property" type="text"/>
						</div>

						<div className="flex-label">
							<label htmlFor="property-album">Album</label>
							<input id="property-album" name="property-album" className="long-property" type="text"/>
						</div>


						<div className="album-wrapper">

							<div>
								<div className="flex-label">
									<label htmlFor="property-genre">Genre</label>
									<input id="property-genre" name="property-genre" className="medium-property" type="text"/>
								</div>
								<div className="flex-label">
									<label htmlFor="property-track-num">Track #</label>
									<input id="property-track-num" name="property-track-num" className="short-property" type="text"/>
								</div>
								<div className="flex-label">
									<label htmlFor="property-year">Year</label>
									<input id="property-year" name="property-year" className="short-property" type="text"/>
								</div>
								<div className="flex-label">
									<label htmlFor="property-bit-rate">Bit Rate</label>
									<input id="property-bit-rate" name="property-bit-rate" className="short-property" type="text" disabled/>
								</div>
								<div className="flex-label">
									<label htmlFor="property-sample-rate">Sample Rate</label>
									<input id="property-sample-rate" name="property-sample-rate" className="short-property" type="text" disabled/>
								</div>
							</div>

							<div
								className="album-art"
								style={{ backgroundImage: 'url(' + defaultImageLink + ')' }}
							/>

						</div>
						<div className="property-note">
							Note
							<textarea/>
						</div>
					</div>
				</Modal>
			</div>
		)
	}
}
SongProperties.contextType = MusicContext;
