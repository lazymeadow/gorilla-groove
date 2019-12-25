import React, {useContext} from 'react';
import {withRouter} from "react-router-dom";
import {Api} from "../../api";
import {PopoutMenu} from "../popout-menu/popout-menu";
import {Settings} from "../settings/settings";
import {InviteUser} from "../invite-user/invite-user";
import {deleteCookie} from "../../cookie";
import {DraftRelease} from "../draft-release/draft-release";
import {PermissionType} from "../../enums/permission-type";
import {UserContext} from "../../services/user-provider";

function LogoutButtonInternal(props) {
	const userContext = useContext(UserContext);

	const logout = event => {
		event.preventDefault();
		Api.post('authentication/logout', {
			token: sessionStorage.getItem('token')
		}).catch((error) => {
			console.error(error)
		}).finally(() => {
			sessionStorage.removeItem('token');
			deleteCookie('cookieToken');
			deleteCookie('loggedInEmail');

			props.history.push('/login'); // Redirect to the login page now that we logged out
		});
	};

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
					{ component: <DraftRelease/>, shouldRender: userContext.hasPermission(PermissionType.WRITE_VERSION_HISTORY) },
					{ text: "Logout", clickHandler: logout }
				]}
			/>
		</div>
	)
}

// This page uses the router history. In order to gain access to the history, the class needs
// to be exported wrapped by the router. Now inside of LogoutButtonInternal, this.props will have a history object
export const UserButton = withRouter(LogoutButtonInternal);
