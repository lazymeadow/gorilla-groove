import React from 'react';
import {withRouter} from "react-router-dom";
import {toast} from "react-toastify";
import {Api} from "../../api";
import {addCookie} from "../../cookie";
import {getDeviceIdentifier} from "../../services/version";
import {DeviceType} from "../../enums/device-type";
import {Modal} from "../modal/modal";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";

class LoginPageInternal extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			isModalOpen: false,
			isLoading: false
		};
	}

	submitLogin(event) {
		event.preventDefault();
		event.stopPropagation();

		let params = {
			email: document.getElementById('email').value,
			password: document.getElementById('password').value,
			deviceId: getDeviceIdentifier(),
			version: __VERSION__,
			deviceType: DeviceType.WEB
		};

		Api.post('authentication/login', params)
			.then(result => {
				// Would be better to have the cookie be HttpOnly, but need to figure out how to send it from the server to do that
				const ninetyDays = 7776000;
				addCookie('cookieToken', result.token, ninetyDays);
				addCookie('loggedInEmail', result.email, ninetyDays);

				this.props.history.push('/'); // Redirect to the main page now that we logged in
			})
			.catch(() => {
				toast.error("Well... that didn't work. Check your inputs");
			})
	}

	submitPasswordReset(event) {
		event.preventDefault();
		event.stopPropagation();

		this.setState({ isLoading: true });

		const email = document.getElementById('reset-email').value;

		if (email.trim().length === 0) {
			toast.info('An email is required');
			return;
		}

		Api.post('password-reset', { email }).then(() => {
			toast.success('Password reset sent. Check your email');
			this.setState({ isLoading: false, isModalOpen: false });
		}).catch(() => {
			toast.error('A password reset could not be sent');
			this.setState({ isLoading: false });
		})
	}

	openModal() {
		const preexistingEmail = document.getElementById('email').value;

		this.setState({ isModalOpen: true }, () => {
			// More jank timeout to put stuff on the end of react's queue of stuff to do.
			// Otherwise the input won't yet be defined.
			setTimeout(() => {
				document.getElementById('reset-email').value = preexistingEmail
			}, 0);
		});
	}

	render() {
		return (
			<div className="full-screen">
				<form onSubmit={this.submitLogin.bind(this)}>
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

						<div className="password-reset">
							<span className="span-link" onClick={this.openModal.bind(this)}>
								Forgot your password?
							</span>
							<Modal
								isOpen={this.state.isModalOpen}
								closeFunction={() => this.setState({ isModalOpen: false })}
							>
								<div className="password-reset-modal">
									<LoadingSpinner
										visible={this.state.isLoading}
										small={true}
									/>
									<form onSubmit={this.submitPasswordReset.bind(this)}>
										<label>Email
											<div>
												<input id="reset-email" className="password-reset-input" type="email"/>
											</div>
										</label>

										<div className="text-center">
											<button type="submit">Reset</button>
										</div>
									</form>
								</div>
							</Modal>
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
