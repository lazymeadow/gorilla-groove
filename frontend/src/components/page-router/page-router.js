import React from 'react';
import {BrowserRouter, Route, Switch} from 'react-router-dom'
import {SiteLayout} from "../../components";
import {SongLinkPlayer} from "../song-link-player/song-link-player";
import {LoginPageWrapper} from "../login-page/login-page-wrapper";

export function PageRouter() {
	return (
		<BrowserRouter>
			<Switch>
				<Route path="/login" component={LoginPageWrapper}/>
				<Route path="/track-link/:trackId" component={SongLinkPlayer}/>
				<Route path="/" component={SiteLayout}/>
				<Route render={() => <h1>Yo dawg where the page at</h1>}/>
			</Switch>
		</BrowserRouter>
	)
}
