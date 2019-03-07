import React from 'react';
import {BrowserRouter, Route, Switch} from 'react-router-dom'
import {LoginPage, SiteLayout} from "../../components";
import {SongLinkPlayer} from "../song-link-player/song-link-player";

export function PageRouter() {
	return (
		<BrowserRouter>
			<Switch>
				<Route path="/login" component={LoginPage}/>
				<Route path="/track-link/:trackId" component={SongLinkPlayer}/>
				<Route path="/" component={SiteLayout}/>
				<Route render={() => <h1>Yo dawg where the page at</h1>}/>
			</Switch>
		</BrowserRouter>
	)
}
