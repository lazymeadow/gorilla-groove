import React from 'react';
import {BrowserRouter, Route, Switch} from 'react-router-dom'
import SongLinkPlayer from "../song-link-player/song-link-player";
import SiteLayout from "../site-layout/site-layout";
import AccountCreation from "../account-creation/account-creation";
import {LoginPage} from "..";
import PrivacyPolicy from "../privacy-policy/privacy-policy";

// NOTE - These routes are served up from the backend in a pretty naive way right now.
// Any additional routes we add need to be whitelisted for unauthenticated users (if applicable)
// and added to ForwardingController.kt so that the frontend's route manager gets the requests
export function PageRouter() {
	return (
		<BrowserRouter>
			<Switch>
				<Route path="/login" component={LoginPage}/>
				<Route path="/privacy-policy" component={PrivacyPolicy}/>
				<Route path="/track-link/:trackId" component={SongLinkPlayer}/>
				<Route path="/create-account/:key" component={AccountCreation}/>
				<Route path="/" component={SiteLayout}/>
				<Route render={() => <h1>Yo dawg where the page at</h1>}/>
			</Switch>
		</BrowserRouter>
	)
}
