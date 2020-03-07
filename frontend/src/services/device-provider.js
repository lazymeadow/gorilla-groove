import React from "react";
import {Api} from "../api";
import {getCookieValue} from "../cookie";
import {PermissionType} from "../enums/permission-type";
import {getDeviceIdentifier} from "./version";

export const DeviceContext = React.createContext();

let partyModeTimeout = null;

export class DeviceProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			ownDevice: {},
			otherDevices: [],

			loadOtherDevices: (...args) => this.loadOtherDevices(...args),
			loadOwnDevice: (...args) => this.loadOwnDevice(...args),
			isInPartyMode: (...args) => this.isInPartyMode(...args),
			setPartyMode: (...args) => this.setPartyMode(...args),
		}
	}

	loadOtherDevices() {
		return Api.get(`device/active?excluding-device=${getDeviceIdentifier()}`).then(devices => {
			this.setState({ otherDevices: devices });
		});
	}

	loadOwnDevice() {
		const deviceIdentifier = getDeviceIdentifier();
		if (deviceIdentifier === undefined) {
			throw Error('Device identifier not set before loading device!')
		}

		return Api.get(`device/${getDeviceIdentifier()}`).then(ownDevice => {
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
			<DeviceContext.Provider value={this.state}>
				{this.props.children}
			</DeviceContext.Provider>
		)
	}
}
