import React from 'react';
import {withRouter} from "react-router-dom";
import {Api} from "../../api";
import {PopoutMenu} from "../popout-menu/popout-menu";

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

	render() {
		return (
			<div className="user-menu">
				<PopoutMenu
					mainItem={{
						className: "user-button",
						text: sessionStorage.getItem('loggedInUserName')
					}}
					menuItems={[
						{ text: "Settings", clickHandler: () => alert("Settings") },
						{ text: "Logout", clickHandler: (e) => this.logout(e) }
					]}
				/>
				{/*<div className="user-button" onClick={() => this.toggleExpanded()}>*/}
				{/*</div>*/}
			</div>
		)
	}
}

// This page uses the router history. In order to gain access to the history, the class needs
// to be exported wrapped by the router. Now inside of LogoutButtonInternal, this.props will have a history object
export const UserButton = withRouter(LogoutButtonInternal);
