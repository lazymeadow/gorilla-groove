import React from 'react';

export class EditableDiv extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			newValue: this.props.text
		}
	}

	componentDidUpdate(prevProps) {
		// Make sure we just stopped editing the data
		if (prevProps.editable === true && this.props.editable === false) {
			// Make sure the value is new
			if (this.state.newValue !== prevProps.text) {
				this.props.updateHandler(this.state.newValue);
			}
		}
	}

	render() {
		if (this.props.editable) {
			return (
				<input
					id={this.props.id}
					defaultValue={this.props.text}
					onChange={(event) => this.setState({ newValue: event.target.value})}
				/>
			)
		} else {
			return (
				<div id={this.props.id}>
					{this.props.text}
				</div>
			)
		}
	}

}
