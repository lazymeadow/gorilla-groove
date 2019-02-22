import React, {Fragment} from 'react';
import {Api} from "../../api";
import {toast} from "react-toastify";
import {MusicContext} from "../../services/music-provider";

// Processing a file on the server takes some amount of time and we currently have no insight into its progress.
// So just estimate that processing on the server is ~15% of the time taken for now
let uploadTimeWeight = 0.85;

export class SongUpload extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			uploading: false,
			filesToUpload: [],
			songsFinished: 0,
			totalProgress: 0
		};
	}

	// noinspection JSMethodCanBeStatic
	openFileDialog() {
		document.getElementById('file-upload').click();
	}

	handleUploadStart(inputEvent) {
		let files = [...inputEvent.target.files].map(file => {
			return {
				progressPercent: 0,
				uploadStage: FileProgress.NOT_STARTED,
				fileData: file
			};
		});
		this.setState({
			uploading: true,
			filesToUpload: files
		}, this.uploadSongs);
	}

	async uploadSongs() {
		let files = this.state.filesToUpload;
		let successfulFiles = [];

		for (const file of files) {
			file.uploadStage = FileProgress.UPLOADING;
			let uploadedFile = await this.uploadSingleSong(file);
			if (uploadedFile) {
				successfulFiles.push(uploadedFile);
				this.setState({ songsFinished: this.state.songsFinished + 1 })
			}
		}

		this.setState({
			uploading: false,
			filesToUpload: [],
			songsFinished: 0,
			totalProgress: 0
		});

		if (successfulFiles.length === 1) {
			toast.success(`'${successfulFiles[0].fileData.name}' uploaded successfully`)
		} else if (successfulFiles.length > 1) {
			toast.success(`${successfulFiles.length} songs uploaded successfully`)
		}
	}

	uploadSingleSong(file) {
		const name = file.fileData.name;

		return Api.upload(
			'POST',
			'file/upload',
			{ file: file.fileData },
			(e) => this.handleFileUploadProgress(e, file)
		).then((response) => {
			file.uploadStage = FileProgress.DONE;
			file.progressPercent = 100;

			try {
				let trackData = JSON.parse(response);
				this.context.addUploadToExistingLibraryView(trackData);
				return file;
			} catch (e) {
				console.error(e);
				toast.info(`The upload of ${name} succeeded, but failed to be loaded into the library view`);
			}

		}).catch(error => {
			console.error(error);
			toast.error(`The upload of '${name}' failed`);
			file.uploadStage = FileProgress.FAILED;
		});
	}

	handleFileUploadProgress(event, file) {
		let newPercent = parseInt(event.loaded / event.total * 100);
		file.progressPercent = newPercent * uploadTimeWeight;
		if (newPercent === 100) {
			file.uploadStage = FileProgress.PROCESSING;
		}

		this.updateTotalPercentage()
	}

	updateTotalPercentage() {
		let totalSongs = this.state.filesToUpload.length;
		let individualSongWeight = 1 / totalSongs;

		let totalProgress = this.state.filesToUpload.reduce((totalDone, file) => {
			if (file.uploadStage === FileProgress.NOT_STARTED) {
				return totalDone;
			} else if (file.uploadStage === FileProgress.UPLOADING || file.uploadStage === FileProgress.PROCESSING) {
				return totalDone + (file.progressPercent * individualSongWeight);
			}  else {
				return totalDone + (100 * individualSongWeight)
			}
		}, 0);

		this.setState({ totalProgress: totalProgress });
	}

	render() {
		let buttonClass = this.state.uploading ? 'display-none' : '';
		let loaderClass = this.state.uploading ? '' : 'display-none';
		let floatingWindowClass = this.state.filesToUpload.length > 0 ? '' : 'display-none';
		let title = this.state.uploading ? '' : 'Upload songs';
		// noinspection HtmlUnknownTarget
		return (
			<div id="song-upload" className="vertical-center">
				<div className="icon-container">
					<i
						className={`${buttonClass} fas fa-cloud-upload-alt`}
						title={title}
						onClick={() => this.openFileDialog()}
					>
					</i>
					<div className={`${loaderClass}`}>
						<span className="song-progress-counter">
							{this.state.songsFinished + 1} / {this.state.filesToUpload.length}
						</span>
						<progress className={`overall-upload-progress`} value={this.state.totalProgress} max="100"/>
					</div>
					<input
						type="file"
						id="file-upload"
						className="display-none"
						onChange={(e) => this.handleUploadStart(e)}
						multiple
					/>

					<div className={`floating-window ${floatingWindowClass}`}>
						<div className="progress-grid">
							<div className="grid-head">File Name</div>
							<div className="grid-head">Stage</div>
							<div className="grid-head">Progress</div>
							{ this.state.filesToUpload.map((file, index) => {
								return (
									<Fragment key={index}>
										<div>{file.fileData.name}</div>
										<div>{file.uploadStage}</div>
										<div>
											<progress value={file.progressPercent} max="100"/>
										</div>
									</Fragment>
								)
							})}
						</div>
					</div>
				</div>
			</div>
		)
	}
}
SongUpload.contextType = MusicContext;

const FileProgress = Object.freeze({
	NOT_STARTED: 'Waiting',
	UPLOADING: 'Uploading',
	PROCESSING: 'Processing',
	DONE: 'Finished',
	FAILED: 'Failed'
});
