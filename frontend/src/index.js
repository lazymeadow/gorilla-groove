import React from 'react';
import ReactDOM from 'react-dom';
import SiteWrapper from './components/site-wrapper/site-wrapper';
import GorillaGrooveRoot from '../src2/components/GorillaGrooveRoot';


let RootComponent;
if (!!localStorage.getItem('beta-client')) {
	RootComponent = GorillaGrooveRoot;
	document.body.classList.add('beta');
}
else {
	RootComponent = SiteWrapper;
}


ReactDOM.render(
	<RootComponent/>,
	document.getElementById('root')
);
