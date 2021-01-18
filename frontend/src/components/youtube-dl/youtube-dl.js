import React, {useState} from 'react';
import {Api} from "../../api";
import {Modal} from "../modal/modal";
import {toast} from "react-toastify";
import {MusicContext} from "../../services/music-provider";

export class YoutubeDlModal extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			modalOpen: false,
			cropArtToSquare: true
		}
	}

	componentDidMount() {
		if (this.props.initialUrl !== undefined) {
			document.getElementById('song-url').value = this.props.initialUrl;
		}
		if (this.props.initialTitle !== undefined) {
			document.getElementById('song-name').value = this.props.initialTitle;
		}
		if (this.props.initialYear !== undefined) {
			document.getElementById('song-year').value = this.props.initialYear;
		}
	}

	setModalOpen(isOpen) {
		this.setState({ modalOpen: isOpen })
	}

	submitDownloadForm(event) {
		event.preventDefault();
		event.nativeEvent.propagationStopped = true;

		const url = document.getElementById('song-url').value;
		const name = document.getElementById('song-name').value;
		const artist = document.getElementById('song-artist').value;
		const featuring = document.getElementById('song-featuring').value;
		const album = document.getElementById('song-album').value;
		const year = document.getElementById('song-year').value;
		const trackNumber = document.getElementById('song-track-number').value;
		const genre = document.getElementById('song-genre').value;

		const params = {
			url: url,
			cropArtToSquare: this.state.cropArtToSquare
		};

		if (name) {
			params.name = name;
		}
		if (artist) {
			params.artist = artist;
		}
		if (album) {
			params.album = album;
		}
		if (year) {
			params.releaseYear = year;
		}
		if (trackNumber) {
			params.trackNumber = trackNumber;
		}
		if (genre) {
			params.genre = genre;
		}
		if (featuring) {
			params.featuring = featuring;
		}

		this.props.setDownloading(true);
		this.props.closeFunction();

		Api.post('background-task/youtube-dl', params).then(() => {
			toast.info(`YouTube download started`);
		}).catch(error => {
			console.error(error);
			toast.error('The download could not be started');
		}).finally(() => {
			this.props.setDownloading(false);
		});
	}

	render() {
		return (
			<form id="youtube-dl" className="form-modal" onSubmit={this.submitDownloadForm.bind(this)}>
				<div className="flex-label">
					<label htmlFor="song-url">URL</label>
					<input id="song-url" name="song-url" type="text" required/>
				</div>

				<h4>Optional Metadata</h4>
				<p className="subtext">An attempt to automatically parse metadata will be performed if fields are left empty</p>

				<div className="flex-label">
					<label htmlFor="song-name">Name</label>
					<input id="song-name" name="song-name" type="text"/>
				</div>

				<div className="flex-label">
					<label htmlFor="song-artist">Artist</label>
					<input id="song-artist" name="song-artist" type="text"/>
				</div>

				<div className="flex-label">
					<label htmlFor="song-featuring">Featuring</label>
					<input id="song-featuring" name="song-featuring" type="text"/>
				</div>

				<div className="flex-label">
					<label htmlFor="song-album">Album</label>
					<input id="song-album" name="song-album" type="text"/>
				</div>

				<div className="flex-label">
					<label htmlFor="song-year">Release Year</label>
					<input id="song-year" name="song-year" type="text"/>
				</div>

				<div className="flex-label">
					<label htmlFor="song-track-number">Track Number</label>
					<input id="song-track-number" name="song-track-number" type="text"/>
				</div>

				<div className="flex-label">
					<label htmlFor="song-genre">Genre</label>
					<input id="song-genre" name="song-genre" type="text"/>
				</div>

				<hr/>
				<div>
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
				<button>Download Song</button>
			</form>
		)
	}
}
YoutubeDlModal.contextType = MusicContext;

export default function YoutubeDlButton(props) {
	const [modalOpen, setModalOpen] = useState(false);
	const [downloading, setDownloading] = useState(false);

	const closeFunction = () => setModalOpen(false);

	const buttonClass = downloading ? 'display-none' : '';
	const loaderClass = downloading ? '' : 'display-none';
	const title = downloading ? '' : 'Import from YouTube';
	const faIcon = props.iconOverride !== undefined ? props.iconOverride : 'fab fa-youtube';

	const initialUrl = props.video !== undefined ? props.video.videoUrl : undefined;
	const initialTitle = props.video !== undefined ? props.video.title : undefined;
	const initialYear = props.video !== undefined ? (new Date(props.video.publishedAt)).getFullYear() : undefined;

	return (
		<div className="vertical-center youtube-download-button" onClick={() => setModalOpen(true)}>
			<div className="icon-container">
				<i className={`${buttonClass} ${faIcon}`} title={`${title}`}>
					<Modal
						isOpen={modalOpen}
						closeFunction={closeFunction}
						setDownloading={setDownloading}
					>
						{ modalOpen
							? <YoutubeDlModal
								closeFunction={closeFunction}
								setDownloading={setDownloading}
								initialUrl={initialUrl}
								initialTitle={initialTitle}
								initialYear={initialYear}
							/>
							: null
						}
					</Modal>
				</i>

				<img src="./images/ajax-loader.gif" className={`${loaderClass}`}/>
			</div>
		</div>
	)
}
