import React from 'react';
import {MusicContext} from "../../services/music-provider";
import {LoginPage} from "./login-page";

export class LoginPageWrapper extends React.Component {
	constructor(props) {
		super(props);
	}

	componentDidMount() {
		// This should really be handled by the logout button, but because withRouter is stupid, I made a
		// wrapper component that will disconnect the socket on the login page
		// https://github.com/facebook/react/issues/14061
		this.context.disconnectSocket();
	}

	render() {
		return <LoginPage/>
	}
}

LoginPageWrapper.contextType = MusicContext;
