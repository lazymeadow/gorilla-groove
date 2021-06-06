import React, {useState} from 'react';
import {withRouter} from 'react-router-dom';


const Login = ({}) => {
	const [forgotPassword, setForgotPassword] = useState(false);

	const submitLogin = (event) => {
		event.preventDefault();
		event.stopPropagation();

		const form = event.target;

		console.log('login attempt', form.email.value, form.password.value);
	};

	// const submitPasswordReset = (event) => {
	// 	event.preventDefault();
	// 	event.stopPropagation();
	// }

	const renderLoginForm = () => {
		return (
			<>
				<form onSubmit={submitLogin}>
					<div className={'form-group'}>
						<label htmlFor={'email'}>Email</label>
						<input id={'email'} name={'email'} type={'email'}/>
					</div>
					<div className={'form-group'}>
						<label htmlFor={'password'}>Password</label>
						<input id={'password'} name={'password'} type={'password'}/>
					</div>
					<div className={'form-buttons'}>
						<button className={'primary'} type={'submit'}>Let's groove</button>
					</div>
				</form>
				<p>
					<button className={'link'} onClick={() => setForgotPassword(true)}>Forgot your password?</button>
				</p>
			</>
		);
	};

	const renderForgotPasswordForm = () => {
		return (
			<>
				<p>
					J/K this doesn't work yet, come back later.
				</p>
				<button className={'link'} onClick={() => setForgotPassword(false)}>Go back</button>
			</>
		);
	};

	return (
		<div className={'login'}>
			<h1>Gorilla Groove</h1>
			{forgotPassword ? renderForgotPasswordForm() : renderLoginForm()}
			<div className={'leave-beta'}>
				<button className={'small'} onClick={() => {
					localStorage.removeItem('beta-client');
					window.location.reload();
				}}>
					Leave beta
				</button>
			</div>
		</div>
	);
};

export default withRouter(Login);