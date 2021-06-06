import React from 'react';
import {BrowserRouter, Route, Switch} from 'react-router-dom';
import Login from './Login/Login';
import MainPage from './MainPage/MainPage';


const GorillaGrooveRoot = ({onLeaveBeta}) => {
	return (
		<>
			<div className={'beta-warning'}>
				<p>
					You're currently using the beta version of <em>Gorilla Groove: Ultimate</em>.
				</p>
				<button className={'small'} onClick={onLeaveBeta}>
					Yeet
				</button>
			</div>
			<BrowserRouter>
				<Switch>
					<Route path='/login' component={Login}/>
					<Route path='/' component={MainPage}/>
					<Route render={() => <h1>Yo dawg where the page at</h1>}/>
				</Switch>
			</BrowserRouter>
		</>
	);
};

export default GorillaGrooveRoot;