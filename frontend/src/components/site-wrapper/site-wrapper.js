import React from 'react';
import {MusicProvider} from "../../services/music-provider";
import {Slide, ToastContainer} from "react-toastify";
import {PageRouter} from "..";

export class SiteWrapper extends React.Component {

	render() {
		return (
			<MusicProvider>
				<ToastContainer autoClose={5000} hideProgressBar={true} transition={Slide}/>
				<PageRouter/>
			</MusicProvider>
		)
	}
}
