import React from 'react';
import {withRouter} from "react-router-dom";
import {Api} from "../../api";
import {PopoutMenu} from "../popout-menu/popout-menu";
import {Settings} from "../settings/settings";
import {InviteUser} from "../invite-user/invite-user";
import {deleteCookie} from "../../cookie";
import {MusicContext} from "../../services/music-provider";

class LogoutButtonInternal extends React.Component {
	constructor(props) {
		super(props);
	}

	logout(event) {
		event.preventDefault();
		Api.post('authentication/logout', {
			token: sessionStorage.getItem('token')
		}).catch((error) => {
			console.error(error)
		}).finally(() => {
			sessionStorage.removeItem('token');
			deleteCookie('cookieToken');
			deleteCookie('loggedInEmail');
			deleteCookie('loggedInUserName');

			this.context.disconnectSocket();
			this.props.history.push('/login'); // Redirect to the login page now that we logged out
		});
	}

	render() {
		return (
			<div className="user-menu">
				<PopoutMenu
					mainItem={{
						className: "user-button",
						text: <i className="fas fa-bars"/>
					}}
					menuItems={[
						{ component: <Settings/> },
						{ component: <InviteUser/> },
						{ text: "Logout", clickHandler: (e) => this.logout(e) }
					]}
				/>
			</div>
		)
	}
}
LogoutButtonInternal.contextType = MusicContext;

// This page uses the router history. In order to gain access to the history, the class needs
// to be exported wrapped by the router. Now inside of LogoutButtonInternal, this.props will have a history object
export const UserButton = withRouter(LogoutButtonInternal);
