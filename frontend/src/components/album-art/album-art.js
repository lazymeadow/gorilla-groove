import React from 'react';
import {Api} from "../../api";
import {MusicContext} from "../../services/music-provider";

let defaultImage = './static/unknown-art.jpg';

export class AlbumArt extends React.Component {
	constructor(props) {
		super(props);
	}

	// noinspection JSMethodCanBeStatic
	addDefaultSrc(event){
		event.target.src = defaultImage;
	};

	render() {
		let userTrack = this.context.playedTrack;
		let imgSrc = userTrack ? Api.getAlbumArtResourceLink(userTrack) : defaultImage;

		return (
			<div className="album-art-container">
				<div className="album-art-header">Album Art</div>
				<img
					className="album-art"
					src={imgSrc}
					onError={this.addDefaultSrc}
				/>
			</div>
		)
	}
}
AlbumArt.contextType = MusicContext;
