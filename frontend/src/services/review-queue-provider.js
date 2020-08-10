import React from "react";
import {Api} from "../api";
import {ReviewSourceType} from "../components/review-queue/review-queue-management/review-queue-management";

export const ReviewQueueContext = React.createContext();

export class ReviewQueueProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			reviewQueueCount: 0,
			queuesFetched: false,
			reviewQueueTracks: [],
			reviewQueueSources: [], // Does not contain USER_RECOMMEND types
			reviewQueueSourceIdToSource: {},
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
		this.setState({ queuesFetched: true });

		return Api.get('review-queue').then(sources => {
			const sourceIdToSource = {};
			sources.forEach(source => {
				sourceIdToSource[source.id] = source;
			});
			this.setState({
				reviewQueueSources: sources.filter(it => it.sourceType !== ReviewSourceType.USER_RECOMMEND),
				reviewQueueSourceIdToSource: sourceIdToSource
			});

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
