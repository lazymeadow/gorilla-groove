export function getDateStringFromUnixTime(unixTime) {
	if (!unixTime) {
		return '';
	}

	let date = new Date(unixTime);
	return `${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}`
}
