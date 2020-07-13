import React from "react";
import {Api} from "../api";

export const ReviewQueueContext = React.createContext();

export class ReviewQueueProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			reviewQueueCount: 0,
			reviewQueueTracks: [],
			reviewQueueSources: [],

			fetchReviewTracks: (...args) => this.fetchReviewTracks(...args),
			fetchReviewQueueSources: (...args) => this.fetchReviewQueueSources(...args)
		}
	}

	fetchReviewTracks() {
		return Api.get('review-queue/track').then(res => {
			const tracks = res.content;

			this.setState({
				reviewQueueCount: res.numberOfElements,
				reviewQueueTracks: tracks
			});

			return tracks
		})
	};

	fetchReviewQueueSources() {
		return Api.get('review-queue/source').then(sources => {
			this.setState({ reviewQueueSources: sources });

			return sources
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
