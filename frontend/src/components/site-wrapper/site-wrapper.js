import React from 'react';
import {MusicProvider} from "../../services/music-provider";
import {Slide, ToastContainer} from "react-toastify";
import {PageRouter} from "..";
import {MusicFilterContext, MusicFilterProvider} from "../../services/music-filter-provider";

export default function SiteWrapper() {

	return (
		// Wrap the music context in the music filter context so it has a reference
		<MusicFilterProvider>
			<MusicFilterContext.Consumer>
				{ musicFilterContext =>
					<MusicProvider filterContext={musicFilterContext}>
						<ToastContainer autoClose={5000} hideProgressBar={true} transition={Slide}/>
						<PageRouter/>
					</MusicProvider>
				}
			</MusicFilterContext.Consumer>
		</MusicFilterProvider>
	)
}
