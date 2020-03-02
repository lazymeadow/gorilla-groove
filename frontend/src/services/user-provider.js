import React from "react";
import {Api} from "../api";
import {getCookieValue} from "../cookie";
import {PermissionType} from "../enums/permission-type";
import {getDeviceIdentifier} from "./version";

export const UserContext = React.createContext();

let partyModeTimeout = null;

export class UserProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			ownUser: {},
			otherUsers: [],
			ownDevice: {},
			ownPermissions: new Set(),

			initialize: (...args) => this.initialize(...args),
			hasPermission: (...args) => this.hasPermission(...args),
			loadOwnDevice: (...args) => this.loadOwnDevice(...args),
			isInPartyMode: (...args) => this.isInPartyMode(...args),
			setPartyMode: (...args) => this.setPartyMode(...args),
		}
	}

	initialize() {
		const permissionPromise = Api.get('user/permissions').then(result => {
			return new Set(result.map(it => PermissionType[it.permissionType]));
		});

		const userPromise = Api.get('user', { showAll: false }).then(allUsers => {
			const loggedInEmail = getCookieValue('loggedInEmail').toLowerCase();
			const ownUserIndex = allUsers.findIndex(user => user.email.toLowerCase() === loggedInEmail);

			if (ownUserIndex === -1) {
				throw Error("Could not locate own user within Gorilla Groove's users");
			} else {
				const ownUser = allUsers.splice(ownUserIndex, 1)[0];
				return { otherUsers: allUsers, ownUser };
			}
		});

		return Promise.all([permissionPromise, userPromise])
			.then(([ownPermissions, {ownUser, otherUsers}]) => {
				this.setState({ ownPermissions, ownUser, otherUsers });
			})
	}

	hasPermission(permissionType) {
		return this.state.ownPermissions.has(permissionType)
	}

	loadOwnDevice() {
		const deviceIdentifier = getDeviceIdentifier();
		if (deviceIdentifier === undefined) {
			throw Error('Device identifier not set before loading device!')
		}
		Api.get(`device/${getDeviceIdentifier()}`).then(ownDevice => {
			this.setState({ ownDevice });

			if (partyModeTimeout !== null) {
				clearTimeout(partyModeTimeout);
			}

			if (!ownDevice.partyEnabledUntil) {
				return;
			}

			const msUntilNoParty = new Date(ownDevice.partyEnabledUntil) - new Date();
			if (msUntilNoParty < 0) {
				return;
			}

			partyModeTimeout = setTimeout(() => {
				this.loadOwnDevice()
			}, msUntilNoParty + 1000)
		});
	}

	isInPartyMode() {
		const partyEnabledUntil = this.state.ownDevice.partyEnabledUntil;
		if (partyEnabledUntil === undefined || partyEnabledUntil === null) {
			return false;
		}

		return (new Date(partyEnabledUntil) - new Date() > 0);
	}

	setPartyMode(enabled, controllingUserIds, msUntilExpiration) {
		return Api.post('device/party', {
			deviceIdentifier: getDeviceIdentifier(),
			enabled,
			controllingUserIds,
			partyUntil: msUntilExpiration !== null ? Date.now() + msUntilExpiration : null
		}).then(updatedDevice => {
			this.setState({ ownDevice: updatedDevice });

			// We want to remove party mode after it expires on our own. So just do a refresh from the server
			// after we set it
			if (partyModeTimeout !== null) {
				clearTimeout(partyModeTimeout);
			}

			partyModeTimeout = setTimeout(() => {
				this.loadOwnDevice()
			}, msUntilExpiration + 1000)
		});
	}

	render() {
		return (
			<UserContext.Provider value={this.state}>
				{this.props.children}
			</UserContext.Provider>
		)
	}
}
