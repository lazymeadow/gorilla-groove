import React, {useContext} from 'react';
import {MusicContext} from "../../services/music-provider";
import {LoginPage} from "./login-page";
import {SocketContext} from "../../services/socket-provider";

export default function LoginPageWrapper() {

	const socketContext = useContext(SocketContext);
	const musicContext = useContext(MusicContext);

	// This should really be handled by the logout button, but because withRouter is stupid, I made a
	// wrapper component that will disconnect the socket on the login page
	// https://github.com/facebook/react/issues/14061
	socketContext.disconnectSocket();

	// It would be nice to put the <MusicContext> around just the logged-in parts of the site.
	// Then whenever you log out, you'll get a fresh Context upon log in. But because of the issue with
	// disconnecting from the Socket (from the comment above this one), and my laziness in dealing with it in
	// a better way, we still need the context available from the login page. So reset the context here manually
	musicContext.resetSessionState();

	return <LoginPage/>;
}
