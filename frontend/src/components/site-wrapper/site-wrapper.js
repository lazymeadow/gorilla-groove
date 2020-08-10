import React from 'react';
import {MusicContext, MusicProvider} from "../../services/music-provider";
import {Slide, ToastContainer} from "react-toastify";
import {PageRouter} from "..";
import {MusicFilterContext, MusicFilterProvider} from "../../services/music-filter-provider";
import {SocketProvider} from "../../services/socket-provider";
import {UserProvider} from "../../services/user-provider";
import {PlaylistProvider} from "../../services/playlist-provider";
import {PlaybackProvider, PlaybackContext} from "../../services/playback-provider";
import {DeviceProvider} from "../../services/device-provider";
import {ReviewQueueProvider} from "../../services/review-queue-provider";

export default function SiteWrapper() {

	return (
		<ReviewQueueProvider>
			<PlaylistProvider>
				<PlaybackProvider>
					<PlaybackContext.Consumer>
						{playbackContext =>
							<UserProvider>
								<DeviceProvider>
									{/*Wrap the music context in the music filter context so it has a reference*/}
									<MusicFilterProvider>
										<MusicFilterContext.Consumer>
											{musicFilterContext =>
												<MusicProvider filterContext={musicFilterContext}>
													<MusicContext.Consumer>
														{musicContext =>
															<SocketProvider musicContext={musicContext} playbackContext={playbackContext}>
																<ToastContainer autoClose={5000} hideProgressBar={true} transition={Slide}/>
																<PageRouter/>
															</SocketProvider>
														}
													</MusicContext.Consumer>
												</MusicProvider>
											}
										</MusicFilterContext.Consumer>
									</MusicFilterProvider>
								</DeviceProvider>
							</UserProvider>
						}
					</PlaybackContext.Consumer>
				</PlaybackProvider>
			</PlaylistProvider>
		</ReviewQueueProvider>
	)
}
