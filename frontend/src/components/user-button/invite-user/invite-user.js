import React, {useState} from 'react';
import {Api} from "../../../api";
import {toast} from "react-toastify";
import {copyToClipboard} from "../../../util";
import {Modal} from "../../modal/modal";

export default function InviteUser() {
	const [inviteLink, setInviteLink] = useState(null);

	const createLink = () => {
		Api.post('user-invite-link').then(res => {
			const completeLink = Api.getBaseUrl() + res.link;
			copyToClipboard(completeLink).then(() => {
				toast.success(`Copied an invitation link to the clipboard\n\nThis link is valid for 7 days`);
			}).catch(() => {
				setInviteLink(completeLink);
			});
		}).catch(error => {
			console.error(error);
			toast.error('Failed to create an invite link');
		});
	};

	return (
		<div>
			<div onClick={() => createLink()}>
				Invite a User
			</div>
			<Modal
				isOpen={inviteLink !== null}
				closeFunction={() => setInviteLink(null)}
			>
				<div>
					Your browser does not support the clipboard API. Please manually copy this link.
				</div>
				<hr/>
				<div>{inviteLink}</div>
			</Modal>
		</div>
	)
}
