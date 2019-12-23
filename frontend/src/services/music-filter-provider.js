import React from "react";

export const MusicFilterContext = React.createContext();

export class MusicFilterProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			searchTerm: '',
			showHidden: false,

			setProviderState: (...args) => this.setProviderState(...args),
			resetState: (...args) => this.resetState(...args),
		}
	}

	setProviderState(state, callback) {
		this.setState(state, callback);
	}

	resetState() {
		this.setState({
			searchTerm: '',
			showHidden: false
		})
	}

	render() {
		return (
			<MusicFilterContext.Provider value={this.state}>
				{this.props.children}
			</MusicFilterContext.Provider>
		)
	}
}
