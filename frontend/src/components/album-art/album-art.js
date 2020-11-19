import React from 'react';
import {MusicContext} from "../../services/music-provider";
import {Modal} from "../modal/modal";

const defaultImageLink = './images/unknown-art.jpg';

export class AlbumArt extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			modalOpen: false,
			imageUrl: props.artLink !== null ? props.artLink : defaultImageLink,
			lastAttemptedLink: null
		}
	}

	shouldComponentUpdate(nextProps, nextState) {
		return nextProps.artLink !== nextState.lastAttemptedLink
			|| this.state.imageUrl !== nextState.imageUrl
			|| this.state.modalOpen !== nextState.modalOpen;
	}

	// Because we're using a background-image and not a <img>, we need to be creative about
	// falling back to our default image. Create an image element and check if the image loads.
	// Depending on if it does, set the URL we want in our state
	componentDidUpdate() {
		// We come through here twice- the first time to check if the link exists, and to adjust the state if it does.
		// Then the second time we come through here because the link updated. We only need to fetch the first time.
		if (this.props.artLink === this.state.lastAttemptedLink) {
			return;
		}

		this.setState({ lastAttemptedLink: this.props.artLink });

		const img = new Image();
		img.src = this.props.artLink;
		img.onload = () => {
			this.setState({ imageUrl: this.props.artLink });
		};
		img.onerror = () => {
			this.setState({ imageUrl: defaultImageLink });
		};
	}

	setModalOpen(isOpen) {
		this.setState({ modalOpen: isOpen })
	}

	render() {
		return (
			<div onClick={() => this.setModalOpen(true)} className="album-art-container">
				{/* Use a background image here because it behaves better at staying within boundaries */}
				<div
					className="album-art"
					style={{ backgroundImage: `url(${this.state.imageUrl})` }}
				/>
				{
					this.state.modalOpen ? <Modal
						isOpen={true}
						closeFunction={() => this.setModalOpen(false)}
					>
						<img className="modal-image" src={this.state.imageUrl}/>
					</Modal> : null
				}

			</div>
		)
	}
}
AlbumArt.contextType = MusicContext;
