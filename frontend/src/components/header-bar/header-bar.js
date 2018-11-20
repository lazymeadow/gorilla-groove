import React from 'react';
import {UserButton, SongUpload} from "..";

export function HeaderBar() {
	return (
		<div>
			<SongUpload/>
			<div style={{float: 'right'}}>
				<UserButton/>
			</div>
		</div>
	)
}
