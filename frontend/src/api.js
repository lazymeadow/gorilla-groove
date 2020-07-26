export class Api {

	static getBaseUrl() {
		const protocol = isLocalEnvironment() ? 'http' : 'https';
		return protocol + "://" + window.location.host;
	}

	static getBaseApiUrl() {
		return this.getBaseUrl() + '/api/'
	}

	static get(url, params) {
		let urlParameters = Api.encodeUriParamsFromObject(params);

		return fetch(this.getBaseApiUrl() + url + urlParameters, {
			method: 'get',
			headers: new Headers({
				'Content-Type': 'application/json'
			}),
			credentials: 'include'
		}).then(res => {
			// The fetch API treats bad response codes like 4xx or 5xx as falling into the then() block
			// I don't like this behavior, since there was an issue. Throw an error so they fall into catch()
			if (!res.ok) {
				throw Error('Http error with status: ' + res.status);
			}

			return res.json()
		})
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
		let headers = { 'Content-Type': 'application/json' };

		return fetch(this.getBaseApiUrl() + url, {
			method: requestType,
			headers: new Headers(headers),
			credentials: 'include',
			body: JSON.stringify(params)
		}).then(res => {
			// The fetch API treats bad response codes like 4xx or 5xx as falling into the then() block
			// I don't like this behavior, since there was an issue. Throw an error so they fall into catch()
			if (!res.ok) {
				return res.text().then(text => {
					if (!text) {
						throw ''
					} else if (JSON.parse(text)) {
						throw text
					} else {
						throw { error: text }
					}
				});
			}

			// There isn't always a response body for POSTs, and calling res.json() will create a parse error
			// in the console, even within a try / catch. Really this is probably an issue with the server
			// and it probably shouldn't return empty responses. But that sounds more difficult to tackle
			return res.text().then(text => {
				return text ? JSON.parse(text) : {}
			})
		});
	};

	static download(url, params) {
		let urlParameters = Api.encodeUriParamsFromObject(params);

		const fullUrl = this.getBaseApiUrl() + url + urlParameters;

		const a = document.createElement("a");
		a.href = fullUrl;
		a.setAttribute("download", "true");
		a.click();
	}

	static upload(httpMethod, url, params, progressCallback) {
		const fullUrl = this.getBaseApiUrl() + url;

		return new Promise((resolve, reject) => {
			let formData = new FormData();

			Object.keys(params).forEach(paramKey => {
				formData.append(paramKey, params[paramKey]);
			});

			let xhr = new XMLHttpRequest();
			xhr.open(httpMethod, fullUrl);
			xhr.withCredentials = true;
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

function isLocalEnvironment() {
	// We only use a port in the UI when doing local dev
	return window.location.host.includes(":");
}
