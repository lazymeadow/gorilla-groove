import React from 'react';
import {BrowserRouter, Switch, Route} from 'react-router-dom';
import Login from './Login/Login';
import MainPage from './MainPage/MainPage';


const GorillaGrooveRoot = () => {
	return (
		<BrowserRouter>
			<Switch>
				<Route path='/login' component={Login}/>
				<Route path='/' component={MainPage}/>
				<Route render={() => <h1>Yo dawg where the page at</h1>}/>
			</Switch>
		</BrowserRouter>
	);
};

export default GorillaGrooveRoot;