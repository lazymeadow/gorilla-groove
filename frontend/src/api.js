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
			method: 'get',
			headers: new Headers({
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${sessionStorage.getItem('token')}`
			}),
			body: JSON.stringify(params)
		}).then(res => res.json())
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
}
