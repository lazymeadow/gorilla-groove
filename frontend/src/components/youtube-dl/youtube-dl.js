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

		this.setState({
			downloading: true,
			modalOpen: false
		});

		const url = document.getElementById('song-url').value;
		const name = document.getElementById('song-name').value;
		const artist = document.getElementById('song-artist').value;
		const album = document.getElementById('song-album').value;
		const year = document.getElementById('song-year').value;
		const trackNumber = document.getElementById('song-track-number').value;

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

	render() {
		let buttonClass = this.state.downloading ? 'hidden' : '';
		let loaderClass = this.state.downloading ? '' : 'hidden';
		return (
			<div className="youtube-dl-wrapper">
				<button className={buttonClass} onClick={() => this.setModalOpen(true)}>
					Download from YouTube
					<Modal
						isOpen={this.state.modalOpen}
						closeFunction={() => this.setModalOpen(false)}
					>
						<form className="form-modal youtube-dl-modal" onSubmit={(e) => this.submitDownloadForm(e)}>
							<div>
								<label htmlFor="song-url">URL</label>
								<input id="song-url" name="song-url" type="text" required/>
							</div>

							<h4>Optional Metadata</h4>

							<div>
								<label htmlFor="song-name">Name</label>
								<input id="song-name" name="song-name" type="text"/>
							</div>

							<div>
								<label htmlFor="song-artist">Artist</label>
								<input id="song-artist" name="song-artist" type="text"/>
							</div>

							<div>
								<label htmlFor="song-album">Album</label>
								<input id="song-album" name="song-album" type="text"/>
							</div>

							<div>
								<label htmlFor="song-year">Release Year</label>
								<input id="song-year" name="song-year" type="text"/>
							</div>

							<div>
								<label htmlFor="song-track-number">Track Number</label>
								<input id="song-track-number" name="song-track-number" type="text"/>
							</div>

							<div>
								<label htmlFor="song-genre">Genre</label>
								<input id="song-genre" name="song-genre" type="text"/>
							</div>

							<button>Download Song</button>
						</form>
					</Modal>
				</button>
				<img src="./images/ajax-loader.gif" className={`${loaderClass}`}/>
			</div>
		)
	}
}
YoutubeDlButton.contextType = MusicContext;
