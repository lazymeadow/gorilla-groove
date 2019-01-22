import React from 'react';
import {Api} from "../../api";
import {Modal} from "../modal/modal";

export class InviteUserButton extends React.Component {
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
			console.error("Passwords do not match");
			return;
		}

		Api.post('user', {
			username: username,
			email: email,
			password: password1
		}).then(() => {
			this.setModalOpen(false);
		}).catch((error) => {
			console.error(error);
		});
	}

	render() {
		return (
			<button className="invite-user-button" onClick={() => this.setModalOpen(true)}>
				Invite a user
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setModalOpen(false)}
				>
					<form className="invite-user-modal" onSubmit={(e) => this.submitInviteForm(e)}>
						<div>
							<label htmlFor="username">Username</label>
							<input id="username" name="username" type="text" maxLength="32" required/>
						</div>

						<div>
							<label htmlFor="email">Email</label>
							<input id="email" name="email" type="email" required/>
						</div>

						<div>
							<label htmlFor="password">Password</label>
							<input id="password" name="password" type="password" minLength="10" required/>
						</div>

						<div>
							<label htmlFor="password2">Password... again</label>
							<input id="password2" name="password2" type="password" minLength="10" required/>
						</div>

						<button>Create User</button>
					</form>
				</Modal>
			</button>
		)
	}
}
