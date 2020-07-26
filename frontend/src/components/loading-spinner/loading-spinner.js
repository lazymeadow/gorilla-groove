import React from 'react';

export class LoadingSpinner extends React.Component {
	constructor(props) {
		super(props);
	}

	render() {
		const smallClass = this.props.small ? 'small-spinner' : '';

		return (
			this.props.visible ? (
				<div className={`loading-spinner ${smallClass}`}>
					<img className="animation-spin" src="/images/logo.png" width="150" height="150"/>
				</div>
			) : null
		)
	}
}
