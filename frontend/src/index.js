import React from 'react';
import ReactDOM from 'react-dom';
import SiteWrapper from "./components/site-wrapper/site-wrapper";

// Was on 16.13.1 before I went all crazy with this experimental version
ReactDOM.unstable_createRoot(
	document.getElementById('root')
).render(
	<SiteWrapper/>
);
