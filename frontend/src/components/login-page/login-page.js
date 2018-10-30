import React from 'react';
import {Link} from "react-router-dom";

export function LoginPage() {
	return (
		<div className="full-screen border-layout">
			<Link to={'/'}>
				Login
			</Link>
		</div>
	)
}
