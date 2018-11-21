import React from 'react';
import {UserButton, SongUpload} from "..";

export function HeaderBar() {
	return (
		<div className="header-bar">
			<SongUpload/>
			<div style={{float: 'right'}}>
				<UserButton/>
			</div>
		</div>
	)
}
