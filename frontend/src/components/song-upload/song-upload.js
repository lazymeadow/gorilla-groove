import React from 'react';
import {Api} from "../../api";

export function SongUpload() {

	function uploadSong(inputEvent) {
		// This can be altered to be a multi-file upload, and perhaps it should be
		let file = inputEvent.target.files[0];

		Api.upload('file/upload', file).then(
			(result) => {
				console.log(result);
			},
			(error) => {
				console.error(error)
			}
		)
	}

	return (
		<input type="file" id="input" onChange={uploadSong}/>
	)
}
