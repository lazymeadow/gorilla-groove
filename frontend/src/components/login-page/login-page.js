import React from 'react';
import {withRouter} from "react-router-dom";
import {toast} from "react-toastify";
import {Api} from "../../api";
import {addCookie} from "../../cookie";

class LoginPageInternal extends React.Component {
	constructor(props) {
		super(props);
	}

	submit(event) {
		event.preventDefault();

		let params = {
			email: document.getElementById('email').value,
			password: document.getElementById('password').value,
		};

		Api.post('authentication/login', params)
			.then(result => {
				// Would be better to have the cookie be HttpOnly, but need to figure out how to send it from the server to do that
				const ninetyDays = 7776000;
				addCookie('cookieToken', result.token, ninetyDays);
				addCookie('loggedInUserName', result.username, ninetyDays);
				addCookie('loggedInEmail', result.email, ninetyDays);

				this.props.history.push('/'); // Redirect to the main page now that we logged in
			})
			.catch(() => {
				toast.error("Well... that didn't work. Check your inputs");
			})
	}

	downloadApk(event) {
		event.preventDefault();

		Api.download('file/download-apk');

		return false;
	}

	render() {
		return (
			<div className="full-screen">
				<form onSubmit={this.submit.bind(this)}>
					<div className="login-container">
						<div>
							<h1>Gorilla Groove</h1>
							<div className="login-flex">
								<div className="flex-label">
									<label htmlFor="email">Enter your email</label>
									<input id="email" name="email" type="email"/>
								</div>

								<div className="flex-label">
									<label htmlFor="password">Enter your password</label>
									<input id="password" name="password" type="password"/>
								</div>
							</div>

							<button>Login</button>
						</div>

						<div className="apk-download">
							Download the <a onClick={this.downloadApk.bind(this)}>Android App</a>
						</div>
					</div>
				</form>
			</div>
		)
	}
}

// This page uses the router history. In order to gain access to the history, the class needs
// to be exported wrapped by the router. Now inside of LoginPageInternal, this.props will have a history object
export const LoginPage = withRouter(LoginPageInternal);
