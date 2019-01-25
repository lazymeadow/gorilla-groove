import React from 'react';
import {BrowserRouter, Route, Switch} from 'react-router-dom'
import {LoginPage, SiteLayout} from "../../components";

export function PageRouter() {
	return (
		<BrowserRouter>
			<Switch>
				<Route path="/login" component={LoginPage}/>
				<Route path="/" component={SiteLayout}/>
				<Route render={() => <h1>Yo dawg where the page at</h1>}/>
			</Switch>
		</BrowserRouter>
	)
}
