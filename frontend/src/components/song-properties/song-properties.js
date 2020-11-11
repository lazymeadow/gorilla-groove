import React from 'react';
import {MusicContext} from "../../services/music-provider";
import {Modal} from "../modal/modal";
import {toast} from "react-toastify";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {Api} from "../../api";

const UNKNOWN_ART_URL = './images/unknown-art.jpg';

export class SongProperties extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			modalOpen: false,
			loading: false,
			name: '',
			artist: '',
			featuring: '',
			album: '',
			genre: '',
			trackNumber: '',
			releaseYear: '',
			bitRate: '',
			sampleRate: '',
			note: '',
			albumArt: null,
			albumArtUrl: '',
			newArtUrl: '',
			cropArtToSquare: false,

			showingAlbumUploadOptions: false,
			showingAlbumArtUrlUploadModal: false,
		};

		this.inputNames = ['name', 'artist', 'featuring', 'album', 'genre', 'trackNumber',
			'releaseYear', 'bitRate', 'sampleRate', 'note'];
	}

	componentDidUpdate(prevProps, prevState) {
		if (this.state.modalOpen && !prevState.modalOpen) {
			this.initializeInputs();
		}
	}

	initializeInputs() {
		const tracks = this.props.getSelectedTracks();
		const newState = {};
		newState.originalValues = {}; // Keep track of original values so we know what to actually update when saving

		let initializeKey = (key) => {
			return tracks.every(track => track[key] === tracks[0][key]) ? tracks[0][key] : '';
		};

		this.inputNames.forEach(inputName => {
			const originalValue = initializeKey(inputName);
			newState[inputName] = originalValue;
			newState.originalValues[inputName] = originalValue;
		});

		// Gets tricky to display album art when more than one track is selected, so only do this if we're messing with 1
		if (tracks.length === 1) {
			Api.get(`file/link/${tracks[0].id}?linkFetchType=ART`).then(links => {
				if (links.albumArtLink !== null) {
					this.setState({ albumArtUrl: links.albumArtLink });
				}
			})
		}

		newState.newArtUrl = '';
		newState.albumArtUrl = UNKNOWN_ART_URL;
		newState.albumArt = null;
		newState.cropArtToSquare = false;

		this.setState(newState);
	}

	setModalOpen(isOpen) {
		this.setState({
			modalOpen: isOpen,
			showingAlbumUploadOptions: false
		})
	}

	inputChange(stateName, event) {
		const newState = {};
		newState[stateName] = event.target.value;

		this.setState(newState);
	}

	updateTracks(event) {
		event.preventDefault();

		const changedProperties = this.getChangedProperties();

		if (Object.keys(changedProperties).length === 0 && this.state.albumArt === null) {
			toast.info("No track data changes were made");
			this.setState({ modalOpen: false });
			return;
		}

		this.setState({ loading: true });

		this.context.updateTracks(
			this.props.getSelectedTracks(),
			this.state.albumArt,
			changedProperties
		).then(() => {
			this.setState({ modalOpen: false });
			this.context.forceTrackUpdate();
			toast.success('Track data updated successfully');

			// We have altered the album art in some way. Refresh the displayed art if we edited the current song.
			if (changedProperties.albumArtUrl !== undefined || this.state.albumArt !== null || changedProperties.cropArtToSquare === true) {
				const currentTrackId = this.context.playedTrack ? this.context.playedTrack.id : undefined;
				if (this.props.getSelectedTracks().some(track => track.id === currentTrackId)) {
					this.context.refreshArtOfCurrentTrack();
				}
			}
		}).finally(() => this.setState({ loading: false }));
	}

	getChangedProperties() {
		const changedProperties = {};

		this.inputNames.forEach(inputName => {
			const originalValue = this.state.originalValues[inputName];
			const currentValue = this.state[inputName];

			if (currentValue !== originalValue) {
				changedProperties[inputName] = currentValue;
			}
		});

		if (this.state.cropArtToSquare) {
			changedProperties.cropArtToSquare = true;
		}

		if (this.state.newArtUrl) {
			changedProperties.albumArtUrl = this.state.newArtUrl;
		}

		return changedProperties;
	}

	// noinspection JSMethodCanBeStatic
	openFileDialog() {
		this.setState({ showingAlbumUploadOptions: false });
		document.getElementById('picture-upload').click();
	}

	openUrlDialog() {
		this.setState({ showingAlbumArtUrlUploadModal: true });
	}

	displayImageOptions() {
		if (this.state.showingAlbumUploadOptions !== true) {
			this.setState({ showingAlbumUploadOptions: true });
		}
	}

	handleUserAlbumArtUrl(e) {
		e.stopPropagation();
		const newState = {
			showingAlbumUploadOptions	: false,
			showingAlbumArtUrlUploadModal: false
		};

		if (this.state.newArtUrl.trim().length > 0) {
			newState.albumArtUrl = this.state.newArtUrl
		} else {
			newState.newArtUrl = '';
		}

		this.setState(newState);
	}

	handlePictureUpload(inputEvent) {
		let file = inputEvent.target.files[0];

		// Preview the the album art for the user before upload
		let reader = new FileReader();
		reader.onload = (e) => {
			this.setState({
				albumArt: file,
				albumArtUrl: e.target.result
			});
		};

		reader.readAsDataURL(file);
	}

	render() {
		// noinspection HtmlUnknownTarget
		return (
			<div onClick={() => {
				if (this.state.modalOpen !== true) {
					this.setModalOpen(true)
				}
			}}>
				Properties
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setModalOpen(false)}
				>
					<div id="song-properties" className="form-modal">
						<LoadingSpinner visible={this.state.loading}/>

						<form onSubmit={e => this.updateTracks(e)}>

							<h2>Track Properties</h2>

							<div className="flex-label">
								<label htmlFor="property-name">Name</label>
								<input
									id="property-name"
									name="property-name"
									className="long-property"
									type="text"
									onChange={(e) => this.inputChange('name', e)}
									value={this.state.name}
								/>
							</div>

							<div className="flex-label">
								<label htmlFor="property-artist">Artist</label>
								<input
									id="property-artist"
									name="property-artist"
									className="long-property"
									type="text"
									onChange={(e) => this.inputChange('artist', e)}
									value={this.state.artist}
								/>
							</div>

							<div className="flex-label">
								<label htmlFor="property-featuring">Featuring</label>
								<input
									id="property-featuring"
									name="property-featuring"
									className="long-property"
									type="text"
									onChange={(e) => this.inputChange('featuring', e)}
									value={this.state.featuring}
								/>
							</div>

							<div className="flex-label">
								<label htmlFor="property-album">Album</label>
								<input
									id="property-album"
									name="property-album"
									className="long-property"
									type="text"
									onChange={(e) => this.inputChange('album', e)}
									value={this.state.album}
								/>
							</div>


							<div className="album-wrapper">

								<div>
									<div className="flex-label">
										<label htmlFor="property-genre">Genre</label>
										<input
											id="property-genre"
											name="property-genre"
											className="medium-property"
											type="text"
											onChange={(e) => this.inputChange('genre', e)}
											value={this.state.genre}
										/>
									</div>
									<div className="flex-label">
										<label htmlFor="property-track-num">Track #</label>
										<input
											id="property-track-num"
											name="property-track-num"
											className="short-property"
											type="text"
											onChange={(e) => this.inputChange('trackNumber', e)}
											value={this.state.trackNumber}
										/>
									</div>
									<div className="flex-label">
										<label htmlFor="property-year">Year</label>
										<input
											id="property-year"
											name="property-year"
											className="short-property"
											type="text"
											onChange={(e) => this.inputChange('releaseYear', e)}
											value={this.state.releaseYear}
										/>
									</div>
									<div className="flex-label">
										<label htmlFor="property-bit-rate">Bit Rate</label>
										<input
											id="property-bit-rate"
											name="property-bit-rate"
											className="short-property"
											type="text"
											value={this.state.bitRate}
											disabled
										/>
									</div>
									<div className="flex-label">
										<label htmlFor="property-sample-rate">Sample Rate</label>
										<input
											id="property-sample-rate"
											name="property-sample-rate"
											className="short-property"
											type="text"
											value={this.state.sampleRate}
											disabled
										/>
									</div>

									<hr/>
									<div className="flex-label">
										Crop Art to Square?
										<input
											id="crop-yes"
											type="radio"
											name="crop"
											value="true"
											checked={this.state.cropArtToSquare}
											onChange={() => this.setState({ cropArtToSquare: true })}
										/>
										<label htmlFor="crop-yes">Yes</label>

										<input
											id="crop-no"
											type="radio"
											name="crop"
											value="false"
											checked={!this.state.cropArtToSquare}
											onChange={() => this.setState({ cropArtToSquare: false })}
										/>
										<label htmlFor="crop-no">No</label>
									</div>
								</div>

								<div
									className="album-art"
									style={{ backgroundImage: 'url(' + this.state.albumArtUrl + ')' }}
									onClick={this.displayImageOptions.bind(this)}
								>
									{ /* Use a div with background image because it fits bounds better. But use img here that's hidden for onError */ }
									<img
										src={this.state.albumArtUrl}
										className="d-none"
										onError={() => {
											console.error('An invalid image URL was supplied. Reverting image to default.');
											toast.error('The supplied URL was not a valid image');
											this.setState({
												albumArtUrl: UNKNOWN_ART_URL,
												newArtUrl: ''
											})
										}}
									/>

									<div id="image-upload-options" className={this.state.showingAlbumUploadOptions ? '' : 'd-none'}>
										<h3>Upload Art</h3>
										<div className="art-upload-buttons">
											<button type="button" onClick={this.openUrlDialog.bind(this)}>
												From URL
											</button>
											<button type="button" onClick={this.openFileDialog.bind(this)}>
												From file
											</button>
										</div>
									</div>
								</div>
								<input
									type="file"
									id="picture-upload"
									className="display-none"
									onChange={this.handlePictureUpload.bind(this)}
								/>

							</div>
							<div className="property-note">
								Note
								<textarea
									value={this.state.note}
									onChange={e => this.inputChange('note', e)}
								/>
							</div>

							<div>
								<button type="submit">Save</button>
							</div>

							<Modal
								isOpen={this.state.showingAlbumArtUrlUploadModal}
								closeFunction={() => this.setState({ showingAlbumArtUrlUploadModal: false })}
							>
								<form className="form-modal" onSubmit={this.handleUserAlbumArtUrl.bind(this)}>
									<h3>Album Art URL</h3>
									<input
										className="long-property d-block"
										placeholder="https://upload.wikimedia.org/wikipedia/en/3/34/RickAstleyNeverGonnaGiveYouUp7InchSingleCover.jpg"
										onChange={(e) => this.inputChange('newArtUrl', e)}
										value={this.state.newArtUrl}
									/>

									<div className="text-center">
										<button type="submit">Enter</button>
									</div>
								</form>
							</Modal>

						</form>
					</div>
				</Modal>

			</div>
		)
	}
}
SongProperties.contextType = MusicContext;
