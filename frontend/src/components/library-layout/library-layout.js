import React from 'react';
import {LibraryList, PlaybackControls} from "..";

export class LibraryLayout extends React.Component {
	render() {
		return <div className="full-screen border-layout">
			<div className="border-layout-north">
				Some dope ass header stuff
			</div>
			<div className="border-layout-west">
				West
			</div>
			<div className="border-layout-center">
				<LibraryList/>
			</div>
			<div className="border-layout-east">
				East
			</div>
			<div className="border-layout-south">
				<PlaybackControls/>
			</div>
		</div>;
	}
}