import React, {useContext} from 'react';
import {useHistory} from "react-router-dom";
import {Api} from "../../api";
import PopoutMenu from "../popout-menu/popout-menu";
import {Settings} from "../settings/settings";
import InviteUser from "./invite-user/invite-user";
import {deleteCookie} from "../../cookie";
import {DraftRelease} from "../draft-release/draft-release";
import {PermissionType} from "../../enums/permission-type";
import {UserContext} from "../../services/user-provider";
import TrackHistory from "../track-history/track-history";
import DeviceManagement from "../device-management/device-management";

const originalTitle = document.title;

export default function UserButton() {
	const userContext = useContext(UserContext);
	const history = useHistory();

	const logout = event => {
		document.title = originalTitle;

		event.preventDefault();
		Api.post('authentication/logout', {
			token: sessionStorage.getItem('token')
		}).catch(error => {
			console.error(error)
		}).finally(() => {
			sessionStorage.removeItem('token');
			deleteCookie('cookieToken');
			deleteCookie('loggedInEmail');

			history.push('/login'); // Redirect to the login page now that we logged out
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
					{ component: <DeviceManagement/> },
					{ component: <TrackHistory/> },
					{ component: <InviteUser/>, shouldRender: userContext.hasPermission(PermissionType.INVITE_USER)  },
					{ component: <DraftRelease/>, shouldRender: userContext.hasPermission(PermissionType.WRITE_VERSION_HISTORY) },
					{ text: "Logout", clickHandler: logout }
				]}
			/>
		</div>
	)
}
