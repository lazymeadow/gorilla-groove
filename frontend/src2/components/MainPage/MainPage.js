import React from 'react';
import {withRouter} from 'react-router-dom';
import {deleteCookie} from '../../../src/cookie';
import {Api} from '../../util/api';
import Logo from '../Logo';


const MainPage = ({history}) => {
	const handleLogOut = async (event) => {
		event.preventDefault();
		try {
			await Api.post('authentication/logout');
		} catch (error) {
			console.error(error);
		} finally {
			deleteCookie('cookieToken');
			deleteCookie('loggedInEmail');

			history.push('/login');
		}
	};

	return (
		<div className={'main'}>
			<div>
				<div className={'nav'}>
					<Logo />
					<p>This is where things will live, such as</p>
					<button className={'link'} onClick={handleLogOut}>
						Log out
					</button>
				</div>
				<div className={'panel'}>
					<p>this is where the main content will display</p>
				</div>
			</div>
			<div className={'footer'}>
				<p>this is where play controls will be</p>
			</div>
		</div>
	);
};

export default withRouter(MainPage);