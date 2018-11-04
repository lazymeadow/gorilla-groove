import React from 'react';
import {LibraryList, PlaybackControls, SongUpload, Api} from "..";
import {Link} from "react-router-dom";

export class LibraryLayout extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			isLoaded: false, // TODO use this to actually indicate loading
			userTracks: []
		}
	}

	componentDidMount() {
		Api.get("library").then(
			(result) => {
				this.setState({userTracks: result.content});
			},
			(error) => {
				console.error(error)
			}
		)
	}

	render() {
		return <div className="full-screen border-layout">
			<div className="border-layout-north">
				<Link to={'/login'}>
					Logout
				</Link>
				<SongUpload/>
			</div>
			<div className="border-layout-west">
				West
			</div>
			<div className="border-layout-center">
				<LibraryList userTracks={this.state.userTracks}/>
			</div>
			<div className="border-layout-east">
				East
			</div>
			<div className="border-layout-south">
				<PlaybackControls/>
			</div>
		</div>;
	}
}
