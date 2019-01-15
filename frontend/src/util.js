export function range(start, count) {
	return Array(count)
		.fill(0)
		.map((_, index) => { return index + start;});
}
