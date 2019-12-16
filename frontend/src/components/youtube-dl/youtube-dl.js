import React from 'react';
import {Api} from "../../api";
import {Modal} from "../modal/modal";
import {toast} from "react-toastify";
import {MusicContext} from "../../services/music-provider";

export class YoutubeDlButton extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			modalOpen: false,
			downloading: false
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

		if (url.includes("&list")) {
			toast.error("Playlist downloads are not supported");
			return;
		}

		let params = { url: url };
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

		this.setState({
			downloading: true,
			modalOpen: false
		});

		Api.post('track/youtube-dl', params).then(track => {
			this.context.addUploadToExistingLibraryView(track);
			toast.success(`YouTube song downloaded successfully`);
		}).catch(error => {
			console.error(error);
			toast.error('The download from YouTube failed');
		}).finally(() => {
			this.setState({ downloading: false });
		});
	}

	// When we push enter, a submit event is fired and a key event
	// Wrap the entire form in a key listener that stops the propagation of the key event so songs don't play from 'enter'
	stopPropagation(event) {
		event.nativeEvent.propagationStopped = true;
	}

	render() {
		let buttonClass = this.state.downloading ? 'display-none' : '';
		let loaderClass = this.state.downloading ? '' : 'display-none';
		let title = this.state.downloading ? '' : 'Download from YouTube';

		return (
			<div className="vertical-center" onClick={() => this.setModalOpen(true)}>
				<div className="icon-container" onKeyDown={this.stopPropagation.bind(this)}>
					<i className={`${buttonClass} fab fa-youtube`} title={`${title}`}>
						<Modal isOpen={this.state.modalOpen} closeFunction={() => this.setModalOpen(false)}>
							<form className="form-modal" onSubmit={e => this.submitDownloadForm(e)}>
								<div className="flex-label">
									<label htmlFor="song-url">URL</label>
									<input id="song-url" name="song-url" type="text" required/>
								</div>

								<h4>Optional Metadata</h4>

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

								<button>Download Song</button>
							</form>
						</Modal>
					</i>
					<img src="./images/ajax-loader.gif" className={`${loaderClass}`}/>
				</div>
			</div>
		)
	}
}
YoutubeDlButton.contextType = MusicContext;
