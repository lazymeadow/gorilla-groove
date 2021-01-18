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
import {ReviewQueueProvider, ReviewQueueContext} from "../../services/review-queue-provider";
import {BackgroundTaskProvider, BackgroundTaskContext} from "../../services/background-task-provider";

export default function SiteWrapper() {

	return (
		<ReviewQueueProvider>
			<ReviewQueueContext.Consumer>
				{reviewQueueContext =>
					<PlaylistProvider>
						<BackgroundTaskProvider>
							<BackgroundTaskContext.Consumer>
								{backgroundTaskContext =>
									<PlaybackProvider>
										<PlaybackContext.Consumer>
											{playbackContext =>
												<UserProvider>
													<DeviceProvider>
														<MusicFilterProvider>
															<MusicFilterContext.Consumer>
																{musicFilterContext =>
																	<MusicProvider filterContext={musicFilterContext}>
																		<MusicContext.Consumer>
																			{musicContext =>
																				<SocketProvider
																					musicContext={musicContext}
																					playbackContext={playbackContext}
																					reviewQueueContext={reviewQueueContext}
																					backgroundTaskContext={backgroundTaskContext}
																				>
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
								}
							</BackgroundTaskContext.Consumer>
						</BackgroundTaskProvider>
					</PlaylistProvider>
				}
			</ReviewQueueContext.Consumer>
		</ReviewQueueProvider>
	)
}
