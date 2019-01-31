import React from 'react';
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";
import {Modal} from "../modal/modal";

let defaultImageLink = './images/unknown-art.jpg';

export class Settings extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			modalOpen: false,
		}
	}

	componentDidUpdate() {

	}

	setModalOpen(isOpen) {
		this.setState({ modalOpen: isOpen })
	}

	render() {
		return (
			<div onClick={() => this.setModalOpen(true)}>
				Settings
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setModalOpen(false)}
				>
					<h2>Settings</h2>
					<div>
						<ul>
							<li>Name</li>
							<li>Artist</li>
							<li>Album</li>
							<li>Track #</li>
							<li>Length</li>
							<li>Year</li>
							<li>Genre</li>
							<li>Play Count</li>
							<li>Bit Rate</li>
							<li>Sample Rate</li>
							<li>Added</li>
							<li>Last Played</li>
							<li>Note</li>
						</ul>
					</div>
				</Modal>
			</div>
		)
	}
}
Settings.contextType = MusicContext;
