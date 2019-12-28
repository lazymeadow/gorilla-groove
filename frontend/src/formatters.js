export function formatDate(dateString, includeTime) {
	if (!dateString) {
		return '';
	}

	const date = new Date(dateString);
	const dateResult = `${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}`;

	const timeResult = includeTime
		? ` ${date.toTimeString().split(' ')[0]}`
		: '';

	return dateResult + timeResult;
}

export function formatTimeFromSeconds(lotsOfSeconds) {
	lotsOfSeconds = parseInt(lotsOfSeconds);

	const minutes = parseInt(lotsOfSeconds / 60);
	const seconds = lotsOfSeconds % 60;
	const paddedSeconds = (seconds < 10 ? '0' : '') + seconds;
	return `${minutes}:${paddedSeconds}`;
}
