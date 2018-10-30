import React from 'react';
import {BrowserRouter as Router, Route} from 'react-router-dom'
import {LoginPage, LibraryLayout} from "../../components";

export function PageRouter() {
	return (
		<Router>
			<div>
				<Route exact path="/login" render={props => <LoginPage {...props}/>}/>
				<Route exact path="/" component={LibraryLayout}/>
			</div>
		</Router>
	)
}
