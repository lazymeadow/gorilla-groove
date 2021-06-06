import React from 'react';
import ReactDOM from 'react-dom';
import SiteWrapper from './components/site-wrapper/site-wrapper';
import GorillaGrooveRoot from '../src2/components/GorillaGrooveRoot';
import * as LocalStorage from './local-storage';


let RootComponent;
if (LocalStorage.getBoolean('beta-client')) {
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
