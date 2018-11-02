import React from 'react';
import {withRouter} from "react-router-dom";

class LoginPageInternal extends React.Component {
	constructor(props) {
		super(props);
	}

	submit(event) {
		event.preventDefault();
		fetch('http://localhost:8080/api/authentication/login', {
			method: 'post',
			headers: new Headers({
				'Content-Type': 'application/json'
			}),
			body: JSON.stringify({
				email: document.getElementById('email').value,
				password: document.getElementById('password').value,
			})
		}).then(res => res.json())
			.then(
				(result) => {
					// Would be a little more secure to store in an httpOnly cookie, but the inconvenience of reworking things at
					// the moment is really not worth it for a website such as this where an XSS compromise doesn't really matter
					sessionStorage.setItem('token', result.token);
					this.props.history.push('/'); // Redirect to the main page now that we logged in
				},
				(error) => {
					console.error(error)
				});
	}

	render() {
		return (
			<div className="full-screen">
				<form onSubmit={this.submit.bind(this)}>
					<label htmlFor="email">Enter your email</label>
					<input id="email" name="email" type="email"/>

					<label htmlFor="password">Enter your password</label>
					<input id="password" name="password" type="password"/>

					<button>Login</button>
				</form>
			</div>
		)
	}
}

// This page uses the router history. In order to gain access to the history, the class needs
// to be exported wrapped by the router. Now inside of LoginPageInternal, this.props will have a history object
export const LoginPage = withRouter(LoginPageInternal);
