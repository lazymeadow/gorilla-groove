export class Api {

	static getBaseHost() {
		if (window.location.host.includes("localhost")) {
			// For local dev-ing, I usually run react on a different web server. So redirect it to
			// the one running the backend on 8080
			return "http://localhost:8080";
		} else {
			return "http://" + window.location.host;
		}
	}

	static getBaseUrl() {
		return this.getBaseHost() + '/api/'
	}

	static get(url, params) {
		let urlParameters = Api.encodeUriParamsFromObject(params);

		return fetch(this.getBaseUrl() + url + urlParameters, {
			method: 'get',
			headers: new Headers({
				'Content-Type': 'application/json',
				'Authorization': `Bearer ${sessionStorage.getItem('token')}`
			}),
		}).then(res => res.json())
	}

	static put(url, params) {
		return Api.sendRequest('put', url, params);
	}

	static post(url, params) {
		return Api.sendRequest('post', url, params);
	}

	static delete(url, params) {
		return Api.sendRequest('delete', url, params);
	}

	static sendRequest(requestType, url, params) {
		let headers = { 'Content-Type': 'application/json', };
		if (sessionStorage.getItem('token')) {
			headers['Authorization'] = `Bearer ${sessionStorage.getItem('token')}`;
		}

		return fetch(this.getBaseUrl() + url, {
			method: requestType,
			headers: new Headers(headers),
			body: JSON.stringify(params)
		}).then(res => {
			// The fetch API treats bad response codes like 4xx or 5xx as falling into the then() block
			// I don't like this behavior, since there was an issue. Throw an error so they fall into catch()
			if (!res.ok) {
				throw Error(res.statusText)
			}

			// There isn't always a response body for POSTs, and calling res.json() will create a parse error
			// in the console, even within a try / catch. Really this is probably an issue with the server
			// and it probably shouldn't return empty responses. But that sounds more difficult to tackle
			return res.text().then(function(text) {
				return text ? JSON.parse(text) : {}
			})
		})
	};

	static upload(httpMethod, url, params, progressCallback) {
		let fullUrl = this.getBaseUrl() + url;

		return new Promise(function (resolve, reject) {
			let formData = new FormData();

			Object.keys(params).forEach(paramKey => {
				formData.append(paramKey, params[paramKey]);
			});

			let xhr = new XMLHttpRequest();
			xhr.open(httpMethod, fullUrl);
			xhr.setRequestHeader('Authorization', `Bearer ${sessionStorage.getItem('token')}`);
			xhr.upload.addEventListener("progress", progressCallback, false);
			xhr.onload = function () {
				if (this.status >= 200 && this.status < 300) {
					resolve(xhr.response);
				} else {
					reject({
						status: this.status,
						statusText: xhr.statusText
					});
				}
			};
			xhr.onerror = function () {
				reject({
					status: this.status,
					statusText: xhr.statusText
				});
			};
			xhr.send(formData);
		});
	}

	static encodeUriParamsFromObject(params) {
		if (!params || Object.keys(params).length === 0) {
			return '';
		}

		let queryString = Object.keys(params).map(key => {
			// Spring wants multiple sort terms passed in like
			// sort=title,asc&sort=album,asc&sort=artist,asc
			// so repeat the sort term for each item, if we are dealing with the sort term
			if (key === 'sort') {
				return params[key].map(sortItem => {
					return `sort=${sortItem}`
				}).join('&')
			} else {
				let encodedKey = encodeURIComponent(key);
				let encodedValue = encodeURIComponent(params[key]);
				return `${encodedKey}=${encodedValue}`
			}
		}).join('&');

		return '?' + queryString;
	}
}
