import React from 'react';
import {withRouter} from "react-router-dom";
import {Api} from "../../api";
import {PopoutMenu} from "../popout-menu/popout-menu";
import {Settings} from "../settings/settings";
import {InviteUser} from "../invite-user/invite-user";

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
			this.props.history.push('/login'); // Redirect to the login page now that we logged out
		});
	}

	test(event) {
		event.preventDefault();
		this.props.history.push('/track-link/15');
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

// This page uses the router history. In order to gain access to the history, the class needs
// to be exported wrapped by the router. Now inside of LogoutButtonInternal, this.props will have a history object
export const UserButton = withRouter(LogoutButtonInternal);
