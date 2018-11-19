import React from 'react';
import {LibraryList} from "..";

export function NowPlayingList(props) {
	return (
		// Most of this is just a pass-through of props from the LibraryLayout
		<div className="now-playing-wrapper">
			<div className="now-playing-heading">
				Now Playing
			</div>
			<LibraryList
				columns={props.columns}
				userTracks={props.userTracks}
				playedTrackIndex={props.playedTrackIndex}
				playedTrack={props.playedTrack}
				playTrack={props.playTrack}/>
		</div>
	)
}
