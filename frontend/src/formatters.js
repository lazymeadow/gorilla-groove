export function formatDateFromUnixTime(unixTime) {
	if (!unixTime) {
		return '';
	}

	let date = new Date(unixTime);
	return `${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}`
}

export function formatTimeFromSeconds(lotsOfSeconds) {
	let minutes = parseInt(lotsOfSeconds / 60);
	let seconds = lotsOfSeconds % 60;
	let paddedSeconds = (seconds < 10 ? '0' : '') + seconds;
	return `${minutes}:${paddedSeconds}`;
}
