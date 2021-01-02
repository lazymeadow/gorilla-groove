import React, {useContext, useState} from 'react';
import {SocketContext} from "../../services/socket-provider";

let panicTimeout = null;
const FIVE_SECONDS = 5000;
const FIVE_MINUTES = 5 * 60 * 1000;

export default function DisconnectedWarningBar() {
	const socketContext = useContext(SocketContext);
	const [isConnected, setConnected] = useState(true);
	const [shouldPanic, setShouldPanic] = useState(false);

	// Don't show the banner IMMEDIATELY. Blips could happen. Give it a couple of seconds to recover before
	// we start notifying users of our potential code updates
	const isConnectedRound1 = socketContext.isConnected || !socketContext.initialized;
	if (isConnectedRound1) {
		if (!isConnected) {
			setConnected(true);
			setShouldPanic(false);
			if (panicTimeout !== null) {
				clearTimeout(panicTimeout);
				panicTimeout = null;
			}
		}
	} else if (isConnected) {
		// We fall into this block if we are not currently connected, but our current state thinks we are.
		if (shouldPanic) {
			setShouldPanic(false);
		}

		setTimeout(() => {
			// If we still aren't connected, then update so the banner will show to the user
			if (!socketContext.isConnected) {
				setConnected(false);
				// Largely unnecessary, but start another timeout if they're still offline after 5 minutes
				panicTimeout = setTimeout(() => {
					if (!socketContext.isConnected) {
						setShouldPanic(true);
					}
				}, FIVE_MINUTES);
			}
		}, FIVE_SECONDS)
	}

	const alertClasses = isConnected ? 'd-none' : 'slide-down';
	const color = shouldPanic ? '#F8D7DA' : 'FFF3CD';

	// noinspection HtmlUnknownTarget
	return (
		<div id="disconnected-warning" style={{ backgroundColor: color }} className={alertClasses}>
			<i className="fas fa-exclamation-triangle"/>
			<div className="message-wrapper">
				<div>Lost connection to the server</div>
				{
					shouldPanic
					?	<small>We've been offline for a while, but don't worry. We'll be back Grooving eventually</small>
					: <small>If your internet is fine, new code is likely being deployed. Do not panic</small>
				}
			</div>
		</div>
	)
}
