import {getDeviceIdentifier} from "./version";
import {Api} from "../api";

const ATTEMPT_LIMIT = 5;
const RETRY_WAIT = 45000;

export function markTrackListened(trackId, successHandler) {
	const params = {
		trackId: trackId,
		deviceId: getDeviceIdentifier(),
		timeListenedAt: (new Date()).toISOString(),
		ianaTimezone: Intl.DateTimeFormat().resolvedOptions().timeZone
	};

	function attemptMarking(params, attemptCount) {
		Api.post('track/mark-listened', params)
			.then(successHandler)
			.catch(e => {
				console.error('Failed to update play count. Attempt count is at ' + attemptCount);
				console.error(e);

				if (attemptCount + 1 > ATTEMPT_LIMIT) {
					console.error('Reached max retry limit. Aborting')
				} else {
					setTimeout(() => attemptMarking(params, attemptCount + 1), RETRY_WAIT);
				}
			});
	}

	attemptMarking(params, 1);
}