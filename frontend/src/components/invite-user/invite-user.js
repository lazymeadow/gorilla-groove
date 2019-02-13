import React from 'react';
import {Api} from "../../api";
import {Modal} from "../modal/modal";
import {toast} from "react-toastify";

export class InviteUser extends React.Component {
	constructor(props) {
		super(props);

		this.state = { modalOpen: false }
	}

	setModalOpen(isOpen) {
		this.setState({ modalOpen: isOpen })
	}

	submitInviteForm(event) {
		event.preventDefault();

		const username = document.getElementById('username').value;
		const email = document.getElementById('email').value;
		const password1 = document.getElementById('password').value;
		const password2 = document.getElementById('password2').value;

		if (password1 !== password2) {
			toast.error('The passwords do not match');
			return;
		}

		Api.post('user', {
			username: username,
			email: email,
			password: password1
		}).then(() => {
			this.setModalOpen(false);
			toast.success(`User ${username} created successfully`);
		}).catch((error) => {
			console.error(error);
			toast.error('The creation of a new user failed');
		});
	}

	render() {
		return (
			<div onClick={() => this.setModalOpen(true)}>
				Invite a User
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setModalOpen(false)}
				>
					<form className="form-modal" onSubmit={(e) => this.submitInviteForm(e)}>
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

						<button>Create User</button>
					</form>
				</Modal>
			</div>
		)
	}
}
