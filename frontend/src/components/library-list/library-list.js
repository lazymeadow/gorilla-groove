import React from 'react';

export function LibraryList(props) {
	return (
		props.tracks.map((track) => {
			return <div key={track.id}>{track.track.name} {track.track.artist}</div>
		})
	);
}
