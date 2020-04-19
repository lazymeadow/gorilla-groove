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

export function formatDateEnglish(dateString) {
	if (!dateString) {
		return '';
	}

	const date = new Date(dateString);
	const month = date.toLocaleString('default', { month: 'long' });

	return `${date.getDate()} ${month} ${date.getFullYear()}`;
}

export function formatTimeFromSeconds(lotsOfSeconds) {
	lotsOfSeconds = parseInt(lotsOfSeconds);

	if (isNaN(lotsOfSeconds)) {
		return '0:00'
	}

	const minutes = parseInt(lotsOfSeconds / 60);
	const seconds = lotsOfSeconds % 60;
	const paddedSeconds = (seconds < 10 ? '0' : '') + seconds;
	return `${minutes}:${paddedSeconds}`;
}

export function toTitleCaseFromSnakeCase(snakeCase) {
	return snakeCase.split('_')
		.map(word => word.charAt(0).toUpperCase() + word.substr(1).toLowerCase())
		.join(' ');
}
