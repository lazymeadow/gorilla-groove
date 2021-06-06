import React from 'react';
import {deleteKey} from '../../util/local-storage';
import {withRouter} from 'react-router-dom';
import {deleteCookie} from '../../../src/cookie';


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
			<h1>okey doke</h1>
			<div>
				<button className={'primary'} onClick={handleLogOut}>
					Log out
				</button>
			</div>
			<div>
				<button className={'small'} onClick={() => {
					deleteKey('beta-client');
					history.push('/');
				}}>
					Leave Beta
				</button>
			</div>
		</div>
	);
};

export default withRouter(MainPage);