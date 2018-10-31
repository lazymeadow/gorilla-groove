export function getDateStringFromUnixTime(unixTime) {
	if (!unixTime) {
		return '';
	}

	let date = new Date(unixTime);
	return `${date.getDay()}/${date.getMonth()}/${date.getFullYear()}`
}
