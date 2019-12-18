import React from 'react';

export class LoadingSpinner extends React.Component {
	constructor(props) {
		super(props);
	}

	render() {
		return (
			this.props.visible ? (
				<div className="loading-spinner">
					<img className="animation-spin" src="./images/logo.png" width="150" height="150"/>
				</div>
			) : <div/>
		)
	}
}
