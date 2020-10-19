import React from "react";
import {Api} from "../api";
import {ReviewSourceType} from "../components/review-queue/review-queue-management/review-queue-management";

export const ReviewQueueContext = React.createContext();

export class ReviewQueueProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			reviewQueueCount: 0,
			viewedReviewSourceId: undefined,
			reviewQueueTracks: [],
			reviewQueueSources: [], // Does not contain USER_RECOMMEND types with no recommends!
			reviewQueueSourceIdToSource: {},
			fetchReviewTracks: (...args) => this.fetchReviewTracks(...args),
			fetchReviewQueueSources: (...args) => this.fetchReviewQueueSources(...args),
			setViewedSourceId: (...args) => this.setViewedSourceId(...args)
		}
	}

	fetchReviewTracks() {
		const url = this.state.viewedReviewSourceId === undefined
			? 'review-queue/track'
			: `review-queue/track/review-source-id/${this.state.viewedReviewSourceId}`;

		return Api.get(url).then(res => {
			const tracks = res.content;

			this.setState({
				reviewQueueTracks: tracks
			});

			return tracks
		});
	};

	setViewedSourceId(viewedSourceId) {
			this.setState({ viewedReviewSourceId: viewedSourceId });
	}

	fetchReviewQueueSources() {
		this.setState({ queuesFetched: true });

		return Api.get('review-queue').then(sources => {
			let totalTracks = 0;

			const sourceIdToSource = {};
			sources.forEach(sourceWithCount => {
				const source = sourceWithCount.reviewSource;
				source.trackCount = sourceWithCount.trackCount;
				totalTracks += source.trackCount;
				sourceIdToSource[source.id] = source;
			});

			const sourcesToAdd = Object.values(sourceIdToSource);

			this.setState({
				reviewQueueSources: sourcesToAdd.sort((a, b) => a.displayName.localeCompare(b.displayName) ),
				reviewQueueSourceIdToSource: sourceIdToSource,
				reviewQueueCount: totalTracks,
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
