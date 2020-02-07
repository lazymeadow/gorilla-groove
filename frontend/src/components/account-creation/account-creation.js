import React, {useEffect, useState} from 'react';
import {toast} from "react-toastify";
import {Api} from "..";
import {useHistory} from "react-router-dom";

export default function AccountCreation(props) {
	const [inviteLink, setInviteLink] = useState({ notLoaded: true });
	const [registrationComplete, setRegistrationComplete] = useState(false);
	const history = useHistory();

	useEffect(() => {
		Api.get('user-invite-link/public/' + props.match.params.key).then(res => {
			console.log(res);
			setInviteLink(res);
		}).catch(error => {
			console.error(error);
			setInviteLink({ notFound: true });
		});
	}, []);

	const createAccount = e => {
		e.preventDefault();

		const username = document.getElementById('username').value;
		const email = document.getElementById('email').value;
		const password1 = document.getElementById('password').value;
		const password2 = document.getElementById('password2').value;

		if (password1 !== password2) {
			toast.info('The passwords do not match');
			return;
		}

		Api.post('user', {
			username: username,
			email: email,
			password: password1,
			inviteLinkIdentifier: props.match.params.key
		}).then(() => {
			setRegistrationComplete(true);
		}).catch(error => {
			console.error(error);
			toast.error('The creation of a new user failed');
		});
	};

	const navigateToLogin = () => {
		history.push('/login');
	};

	const nameEl = document.getElementById('username');
	const newName = nameEl ? nameEl.value : '';

	return (
		<div id="account-creation" className={inviteLink.notLoaded ? 'hidden' : ''}>
			<h2 className={inviteLink.alreadyUsed ? '' : 'display-none'}>
				This invitation has already been used
			</h2>

			<h2 className={inviteLink.notFound ? '' : 'display-none'}>
				No invitation was found with this link. It may have expired
			</h2>

			<div className={registrationComplete || inviteLink.alreadyUsed || inviteLink.notFound ? 'display-none' : ''}>
				<h2>You were invited by {inviteLink.invitingUserName} to <strong>Start Groovin'</strong></h2>
				<form onSubmit={createAccount}>
					<div className="flex-label">
						<label htmlFor="username">Username</label>
						<input id="username" name="username" type="text" maxLength="32" required/>
					</div>

					<div className="flex-label">
						<label htmlFor="email">Email</label>
						<input id="email" name="email" type="email" required/>
					</div>

					<div className="flex-label">
						<label htmlFor="password">Password</label>
						<input id="password" name="password" type="password" minLength="10" required/>
					</div>

					<div className="flex-label">
						<label htmlFor="password2">Password... again</label>
						<input id="password2" name="password2" type="password" minLength="10" required/>
					</div>

					<button>Take me to the Groove Zone</button>
				</form>
			</div>

			<div className={registrationComplete ? 'post-registration-content' : 'transparent'}>
				<h2>Let's begin our Journey, <strong>{newName}</strong></h2>

				<img id="logo" className="animation-pulse" src="../../../images/logo.png"/>

				<div className="confirm-buttons">
					<button onClick={navigateToLogin}>Yes</button>
					<button onClick={navigateToLogin}>Also Yes</button>
				</div>
			</div>
		</div>
	);
}
