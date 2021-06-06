import React, {useEffect, useState} from 'react';
import {withRouter} from 'react-router-dom';
import {deleteCookie} from '../../../src/cookie';
import {Api} from '../../util/api';
import Logo from '../Logo';
import {getDeviceIdentifier} from '../../util';


const MainPage = ({history}) => {
	const [isInitializing, setIsIntializing] = useState(true);

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

	const handleInit = async () => {
		// this is where we will get all the data.
		// 1. load user data
		//   a. permissions - if a request fails, redirect to login
		const permissionsResponse = await Api.get('user/permissions');
		console.log(permissionsResponse);
		//   b. device data
		const deviceResponse = await Api.get('device/active?excluding-device=' + getDeviceIdentifier());
		console.log(deviceResponse)
		// 2. load user's music data
		//   a. library
		//   b. playlists
		//   c. playlist mappings
		//   d. review queues
		setIsIntializing(false);
	}

	useEffect(() => {
		if (isInitializing) {
			// show init message 1 sec + actual load time
			setTimeout(handleInit, 1000);
		}
	}, [isInitializing])

	if (isInitializing) {
		return (
			<div className={'init'}>
				<img src={'../../images/logo.png'} alt={'logo'}/>
				<p>
					Initializing Gorillas...
				</p>
			</div>
		)
	}

	return (
		<div className={'main'}>
			<div>
				<div className={'nav'}>
					<Logo/>
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