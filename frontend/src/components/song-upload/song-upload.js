import React from 'react';

export function SongUpload() {

	function uploadSong(inputEvent) {
		// This can be altered to be a multi-file upload. Perhaps it should be
		let file = inputEvent.target.files[0];
		let body = new FormData();
		body.append('file', file);

		fetch("http://localhost:8080/api/file/upload", {
			method: 'post',
			body: body,
			headers: new Headers({
				'Authorization': 'Bearer df86c467-d940-4239-889f-4d72329f0ba4' // TODO actually authenticate
			})
		}).then(
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
