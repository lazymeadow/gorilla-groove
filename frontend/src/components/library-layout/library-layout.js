import React from 'react';
import {LibraryList, PlaybackControls} from "..";
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
		fetch("http://localhost:8080/api/library", {
			method: 'get',
			headers: new Headers({
				'Authorization': 'Bearer df86c467-d940-4239-889f-4d72329f0ba4' // TODO actually authenticate
			})
		}).then(res => res.json())
			.then(
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
