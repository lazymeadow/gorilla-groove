import React from 'react';

export class EditableDiv extends React.Component {
	constructor(props) {
		super(props);
		this.state = {
			newValue: this.props.text
		}
	}

	shouldComponentUpdate(nextProps) {
		// This is probably gross, but I am choosing to not check the value of the div that we keep in
		// the state to see if we need to render. This is a string comparison, and there could be literally
		// thousands of these divs that need to be checked. Instead, just check the easy conditions in here,
		// and we call forceUpdate() when we have already established a scenario where we know we need to update
		return this.props.editable || nextProps.editable !== this.props.editable
	}

	componentDidUpdate(prevProps) {
		if (prevProps.editable === false && this.props.editable === true) {
			this.setState({ newValue: this.props.text });
		}

		// Make sure we just stopped editing the data
		if (prevProps.editable === true && this.props.editable === false) {
			// Make sure the value is new
			if (this.state.newValue !== prevProps.text) {
				this.props.updateHandler(this.state.newValue);
				this.forceUpdate();
			}
		}
	}

	// This probably could be its own React component... like
	// maybe an <AbbreviatedText/> component or something. But... eh... maybe when I need to reuse it
	handleLongNameTooltip(element) {
		if (!element) {
			return;
		}

		// This isn't very React-y, but because this has to happen after a component renders and knows its own width,
		// it feels cleaner than making a component render twice. Once for initial render, and again to set the title attribute.
		if (this.props.editable === false && element.scrollWidth > element.clientWidth) {
			element.setAttribute('title', this.props.text);
		} else {
			element.removeAttribute('title');
		}
	}

	handleKeyPress(event) {
		if (event.key === 'Enter') {
			this.props.stopEdit();
			event.preventDefault();
			event.nativeEvent.propagationStopped = true;
		} else if (event.key === 'Escape') {
			this.setState({ newValue: this.props.text });
			this.props.stopEdit();
		}
	}

	render() {
		if (this.props.editable) {
			return (
				<input
					id={this.props.id}
					defaultValue={this.props.text}
					onChange={(e) => this.setState({ newValue: e.target.value})}
					onKeyDown={this.handleKeyPress.bind(this)}
					autoFocus
				/>
			)
		} else {
			return (
				<div ref={this.handleLongNameTooltip.bind(this)} id={this.props.id} className="editable-div cutoff-text">
					{this.props.text}
				</div>
			)
		}
	}

}
