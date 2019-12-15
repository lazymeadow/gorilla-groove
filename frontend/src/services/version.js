import {uuidv4} from "../util";
import * as LocalStorage from "../local-storage";
import {Api} from "../api";

// Tell the server what version we're using so we can make decisions about removing backwards compatible stuff
export function notifyVersion() {
	// noinspection JSUnresolvedVariable
	const version = __VERSION__; // Defined in webpack at build time

	Api.put('device', {
		deviceId: getDeviceId(),
		version,
		deviceType: 'WEB'
	})
}

export function getDeviceId() {
	const deviceId = LocalStorage.getString('deviceId');
	if (deviceId) {
		return deviceId;
	}

	const newId = uuidv4();
	LocalStorage.setString('deviceId', newId);

	return newId;
}
