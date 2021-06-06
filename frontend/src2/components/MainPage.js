import React from 'react';


const MainPage = () => {
	return (
		<div>
			<h1>okey doke</h1>

			<button onClick={() => {
				localStorage.removeItem('beta-client');
				window.location.reload();
			}}>
				Leave Beta
			</button>
		</div>
	);
};

export default MainPage;