import React from 'react';
import {withRouter} from 'react-router-dom';


const Login = ({}) => {
	return (
		<div>
			<h1>hey</h1>
			<button onClick={() => {
				localStorage.removeItem('beta-client');
				window.location.reload();
			}}>
				Leave Beta
			</button>
		</div>
	)
};

export default withRouter(Login);