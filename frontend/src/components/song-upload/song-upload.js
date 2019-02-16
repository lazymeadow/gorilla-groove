import React from 'react';
import {Api} from "../../api";
import {toast} from "react-toastify";
import {MusicContext} from "../../services/music-provider";

export class SongUpload extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			uploading: false,
			filesToUpload: []
		}
	}

	// noinspection JSMethodCanBeStatic
	openFileDialog() {
		document.getElementById('file-upload').click();
	}

	handleUploadStart(inputEvent) {
		this.setState({
			uploading: true,
			filesToUpload: inputEvent.target.files
		}, this.uploadSongs());
	}

	async uploadSongs() {
		let files = this.state.filesToUpload;

		for (const file in files) {
			let result = await this.uploadSingleSong(file);
			console.log(result);
		}

		this.setState({ loading: false });
		if (files.length === 1) {
			toast.success(`'${files[0].name}' uploaded successfully`)
		} else {
			toast.success(`${files.length} songs uploaded successfully`)
		}
	}

	uploadSingleSong(file) {
		const name = file.name;

		return Api.upload('file/upload', file).then((response) => {
			response.json().then(track => {
				this.context.addUploadToExistingLibraryView(track);
			}).catch(error => {
				console.error(error);
				toast.info(`The upload of ${name} succeeded, but failed to be loaded into the library view`);
			});
		}).catch(error => {
			console.error(error);
			toast.error(`The upload of '${name}' failed`)
		});
	}

	render() {
		let buttonClass = this.state.uploading ? 'display-none' : '';
		let loaderClass = this.state.uploading ? '' : 'display-none';
		// noinspection HtmlUnknownTarget
		return (
			<div className="vertical-center" title="Upload songs">
				<i className={`${buttonClass} fas fa-cloud-upload-alt`} onClick={() => this.openFileDialog()}>
				</i>
				<img src="./images/ajax-loader.gif" className={`${loaderClass}`}/>
				<input
					type="file"
					id="file-upload"
					className="display-none"
					onChange={(e) => this.handleUploadStart(e)}
					multiple
				/>

				<div>
					Uploading
				</div>
			</div>
		)
	}
}
SongUpload.contextType = MusicContext;
