import React from "react";
import {Api} from "../api";

export const ReviewQueueContext = React.createContext();

export class ReviewQueueProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			reviewQueueCount: 0,
			reviewQueueTracks: [],

			fetchReviewTracks: (...args) => this.fetchReviewTracks(...args)
		}
	}

	fetchReviewTracks() {
		return Api.get('review-queue').then(res => {
			const tracks = res.content;

			this.setState({
				reviewQueueCount: res.numberOfElements,
				reviewQueueTracks: tracks
			});

			return tracks
		})
	};

	render() {
		return (
			<ReviewQueueContext.Provider value={this.state}>
				{this.props.children}
			</ReviewQueueContext.Provider>
		)
	}
}
