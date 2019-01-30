import React from 'react';
import {MusicProvider} from "../../services/music-provider";
import {Slide, toast, ToastContainer} from "react-toastify";
import {PageRouter} from "..";
import {Api} from "../../api";

export class SiteWrapper extends React.Component {

	componentDidMount() {
		Api.get('version').then((serverVersion) => {
			const clientVersion = "1.0.7";
			if (serverVersion.version !== clientVersion) {
				toast.info("Your Gorilla Groove is out of date. Please hard-reload your web page.")
			}
		})
	}

	render() {
		return (
			<MusicProvider>
				<ToastContainer autoClose={5000} hideProgressBar={true} transition={Slide}/>
				<PageRouter/>
			</MusicProvider>
		)
	}
}
