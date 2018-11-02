import React from 'react';
import {Link} from "react-router-dom";

export function LoginPage() {
	function submit(event) {
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
				console.log(result.token);
			},
			(error) => {
				console.error(error)
			});
	}

	return (
		<div className="full-screen">
			<Link to={'/'}>
				Login
			</Link>
			<form onSubmit={submit}>
				<label htmlFor="email">Enter your email</label>
				<input id="email" name="email" type="email"/>

				<label htmlFor="password">Enter your password</label>
				<input id="password" name="password" type="password"/>

				<button>Login</button>
			</form>
		</div>
	)
}
