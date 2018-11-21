import React from 'react';
import {UserButton, SongUpload} from "..";

export function HeaderBar() {
	return (
		<div className="header-bar">
			<SongUpload/>
			<div className="user-button-wrapper">
				<UserButton/>
			</div>
		</div>
	)
}
