import React from 'react';
import {Api} from "../../api";
import {toast} from "react-toastify";
import {MusicContext} from "../../services/music-provider";

export class SongUpload extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			uploading: false
		}
	}

	// noinspection JSMethodCanBeStatic
	openFileDialog() {
		document.getElementById('file-upload').click();
	}

	uploadSong(inputEvent) {
		this.setState({ uploading: true });

		// This can be altered to be a multi-file upload, and perhaps it should be
		let file = inputEvent.target.files[0];
		const name = file.name;

		Api.upload('file/upload', file).then((response) => {
			response.json().then(track => {
				this.context.addUploadToExistingLibraryView(track);
				toast.success(`'${name}' uploaded successfully`)
			}).catch(error => {
				console.error(error);
				toast.info('The song upload succeeded, but the new song could not be loaded');
			});
		}).catch(error => {
			console.error(error);
			toast.error(`The upload of '${name}' failed`)
		}).finally(() => this.setState({ uploading: false }));
	}

	render() {
		let buttonClass = this.state.uploading ? 'hidden' : '';
		let loaderClass = this.state.uploading ? '' : 'hidden';
		// noinspection HtmlUnknownTarget
		return (
			<div className="upload-btn-wrapper">
				<button className={`${buttonClass}`} onClick={() => this.openFileDialog()}>
					Upload a file
				</button>
				<img src="./images/ajax-loader.gif" className={`${loaderClass}`}/>
				<input
					type="file"
					id="file-upload"
					className="display-none"
					onChange={(e) => this.uploadSong(e)}
				/>
			</div>
		)
	}
}
SongUpload.contextType = MusicContext;
