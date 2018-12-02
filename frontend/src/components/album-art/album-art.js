import React from 'react';
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";
import {Modal} from "../modal/modal";

let defaultImage = './static/unknown-art.jpg';

export class AlbumArt extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			modalOpen: false
		}
	}

	// noinspection JSMethodCanBeStatic
	addDefaultSrc(event){
		event.target.src = defaultImage;
	};

	setModalOpen(isOpen) {
		this.setState({ modalOpen: isOpen })
	}

	getImageLink() {
		let userTrack = this.context.playedTrack;
		return userTrack ? Api.getAlbumArtResourceLink(userTrack) : defaultImage;
	}

	render() {
		return (
			<div onClick={() => this.setModalOpen(true)} className="album-art-container">
				<div className="album-art-header">Album Art</div>
				<img
					className="album-art"
					src={this.getImageLink()}
					onError={this.addDefaultSrc}
				/>
				<Modal
					isOpen={this.state.modalOpen}
					closeFunction={() => this.setModalOpen(false)}
				>
				<img className="modal-image" src={this.getImageLink()}/>
			</Modal>
			</div>
		)
	}
}
AlbumArt.contextType = MusicContext;
