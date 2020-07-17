import React from "react";
import {Api} from "../api";
import {getCookieValue} from "../cookie";
import {PermissionType} from "../enums/permission-type";

export const UserContext = React.createContext();

export class UserProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			ownUser: {},
			otherUsers: [],
			ownDevice: {},
			ownPermissions: new Set(),

			initialize: (...args) => this.initialize(...args),
			hasPermission: (...args) => this.hasPermission(...args)
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

	render() {
		return (
			<UserContext.Provider value={this.state}>
				{this.props.children}
			</UserContext.Provider>
		)
	}
}
