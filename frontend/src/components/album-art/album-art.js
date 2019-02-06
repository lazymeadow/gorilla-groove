import React from 'react';
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";
import {Modal} from "../modal/modal";

let defaultImageLink = './images/unknown-art.jpg';

export class AlbumArt extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			modalOpen: false,
			playedTrackId: null,
			imageUrl: defaultImageLink
		}
	}

	shouldComponentUpdate(nextProps, nextState) {
		return (this.context.playedTrack && this.context.playedTrack.id !== this.state.playedTrackId)
			|| this.state.imageUrl !== nextState.imageUrl
			|| this.state.modalOpen !== nextState.modalOpen;
	}


	// Because we're using a background-image and not a <img>, we need to be creative about
	// falling back to our default image. Create an image element and check if the image loads.
	// Depending on if it does, set the URL we want in our state
	componentDidUpdate() {
		if (this.context.playedTrack) {
			this.setState({ playedTrackId: this.context.playedTrack.id });
		} else {
			this.setState({ imageUrl: defaultImageLink });
			return;
		}

		Api.get('file/link/' + this.context.playedTrack.id).then((links) => {
			const albumImageLink = this.getImageLink(links);
			let img = new Image();
			img.src = albumImageLink;
			img.onload = () => {
				this.setState({ imageUrl: albumImageLink })
			};
			img.onerror = () => {
				this.setState({ imageUrl: defaultImageLink })
			};
		});

	}

	setModalOpen(isOpen) {
		this.setState({ modalOpen: isOpen })
	}

	getImageLink(links) {
		// TODO conditional logic if using S3
		return links.albumArtLink + '?t=' + sessionStorage.getItem('token');
	}

	render() {
		return (
			<div onClick={() => this.setModalOpen(true)} className="album-art-container">
				{/* Use a background image here because it behaves better at staying within boundaries */}
				<div
					className="album-art"
					style={{ backgroundImage: 'url(' + this.state.imageUrl + ')' }}
				/>
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setModalOpen(false)}
				>
					<img className="modal-image" src={this.state.imageUrl}/>
				</Modal>
			</div>
		)
	}
}
AlbumArt.contextType = MusicContext;
