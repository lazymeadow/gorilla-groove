import React, {useState} from 'react';
import {Api} from "../../api";
import {Modal} from "../modal/modal";
import {toast} from "react-toastify";
import {MusicContext} from "../../services/music-provider";

export class YoutubeDlModal extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			cropArtToSquare: true,
			confirmationModalOpen: false
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

	submitDownloadForm(event) {
		event.preventDefault();
		event.nativeEvent.propagationStopped = true;

		const url = document.getElementById('song-url').value;

		if (url.includes('&list')) {
			this.setState({ confirmationModalOpen: true });
		} else {
			this.doTheDownload(url);
		}
	}

	confirmationModalCallback(e, userIntent) {
		e.stopPropagation();

		this.setState({ confirmationModalOpen: false });

		const url = document.getElementById('song-url').value;

		// If the user just wanted to download the video (without the playlist) remove the &list from the URL
		if (userIntent === videoOnly) {
			console.log('video only');
			const [urlPath, queryString] = url.split('?');
			const params = new URLSearchParams(queryString);
			params.delete('list');
			params.delete('index');

			console.log(urlPath, queryString);
			console.log(params.toString());
			return this.doTheDownload(urlPath + '?' + params.toString());
		} else if (userIntent === playlist) {
			this.doTheDownload(url);
		}
	}

	doTheDownload(url) {
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
		console.log("About to call close fn'");
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
				<ConfirmPlaylistDownloadModal
					modalOpen={this.state.confirmationModalOpen}
					callback={this.confirmationModalCallback.bind(this)}
				/>

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

const cancel = 2;
const playlist = 1;
const videoOnly = 0;

function ConfirmPlaylistDownloadModal(props) {
	const closeFunction = e => props.callback(e, cancel);

	return (
		<div id="confirm-playlist-download-modal">
			<Modal
				isOpen={props.modalOpen}
				closeFunction={closeFunction}
			>
				{ props.modalOpen ? <div>
					<h3 className="text-center">Playlist Download</h3>
					<p>
						You are about to download every video off of this playlist.<br/>
						Are you sure this is what you want to do?
					</p>
					<div className="flex-between confirm-modal-buttons">
						<button onClick={e => props.callback(e, playlist)}>Yes</button>
						<button onClick={e => props.callback(e, videoOnly)}>No, just the one video only</button>
						<button onClick={e => props.callback(e, cancel)}>What?</button>
					</div>
				</div> : null }
			</Modal>
		</div>
	)
}

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
