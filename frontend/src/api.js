const baseUrl = 'http://localhost:8080/api/';

export class Api {

  // This will likely need to be modified when we start using URL parameters to filter things
	static get(url) {
		return fetch(baseUrl + url, {
			method: 'get',
			headers: new Headers({
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${sessionStorage.getItem('token')}`
			}),
		}).then(res => res.json())
	}

	static post(url, params) {
		return fetch(baseUrl + url, {
			method: 'post',
			headers: new Headers({
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${sessionStorage.getItem('token')}`
			}),
			body: JSON.stringify(params)
		}).then(res => {
			// There isn't always a response body for POSTs, and calling res.json() will create a parse error
			// in the console, even within a try / catch. Really this is probably an issue with the server
			// and it probably shouldn't return empty responses. But that sounds more difficult to tackle
			res.text().then(function(text) {
				return text ? JSON.parse(text) : {}
			})
		})
	}

	static upload(url, file) {
		let body = new FormData();
		body.append('file', file);

		return fetch(baseUrl + url, {
			method: 'post',
			headers: new Headers({
				'Authorization': `Bearer ${sessionStorage.getItem('token')}`
			}),
			body: body
		})
	}

	static getSongResourceLink(fileName) {
		return `${baseUrl}music/${fileName}?t=${sessionStorage.getItem('token')}`;
	}

	static getAlbumArtResourceLink(userTrack) {
		let artId = userTrack.track.id;
		let parentDirectory = parseInt(artId / 1000);
		return `${baseUrl}album-art/${parentDirectory}/${artId}.png?t=${sessionStorage.getItem('token')}`;
	}
}
