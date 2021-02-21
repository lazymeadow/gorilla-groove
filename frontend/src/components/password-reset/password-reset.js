import React, {useEffect, useState} from 'react';
import {Api} from "../../api";
import {toast} from "react-toastify";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {useHistory} from "react-router-dom";

export default function PasswordReset(props) {
	const [message, setMessage] = useState('');
	const [successState, setSuccessState] = useState(false);
	const [changingPassword, setChangingPassword] = useState(false);
	const history = useHistory();

	const uniqueKey = props.match.params.key;

	useEffect(() => {
		Api.get('password-reset/unique-key/' + uniqueKey).then(res => {
			setMessage(`Hi, ${res.username}. Let's change your password`);
			setSuccessState(true);
		}).catch(() => {
			setMessage('Could not find a password reset to use. It probably expired.');
		})
	}, []);

	const changePassword = e => {
		e.preventDefault();

		const password1 = document.getElementById('password').value;
		const password2 = document.getElementById('password2').value;

		if (password1 !== password2) {
			toast.info('The passwords do not match');
			return;
		}

		setChangingPassword(true);

		Api.put('password-reset', { uniqueKey, newPassword: password1 }).then(() => {
			toast.success('Password was changed successfully');

			setTimeout(() => {
				history.push('/')
			}, 2000);
		}).catch(() => {
			toast.error('Password could not be changed');
		}).finally(() => {
			setChangingPassword(false);
		});
	};

	return (
			<div id="password-reset">
				<LoadingSpinner visible={changingPassword}/>

				<h2>{message}</h2>
				{ successState ?
					<div>
						<form onSubmit={changePassword}>
							<div className="flex-label">
								<label htmlFor="password">Password</label>
								<input id="password" name="password" type="password" minLength="9" required/>
							</div>

							<div className="flex-label">
								<label htmlFor="password2">Password... again</label>
								<input id="password2" name="password2" type="password" minLength="9" required/>
							</div>

							<button type="submit">Let's Go</button>
						</form>
					</div>
				: null }
			</div>
	);
}
