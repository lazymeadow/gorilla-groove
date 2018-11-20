import React from 'react';
import {withRouter} from "react-router-dom";
import {Api} from "../../api";

class LogoutButtonInternal extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			expanded: false
		}
	}

	componentDidMount() {
		document.body.addEventListener('click', () => this.closeMenu());
	}

	componentWillUnmount() {
		document.body.removeEventListener('click', () => this.closeMenu());
	}

	closeMenu() {
		this.setState({expanded: false});
	}

	toggleExpanded() {
		this.setState({expanded: !this.state.expanded});
	}

	logout(event) {
		event.preventDefault();
		Api.post('authentication/logout', {
			token: sessionStorage.getItem('token')
		}).catch((error) => {
			console.error(error)
		}).finally(() => {
			sessionStorage.removeItem('token');
			this.props.history.push('/login'); // Redirect to the login page now that we logged out
		});
	}

	render() {
		let menuClass = this.state.expanded ? '' : 'hidden';
		return (
			<div className="user-menu">
				<div className="user-button" onClick={() => this.toggleExpanded()}>
					{sessionStorage.getItem('loggedInUserName')}
				</div>
				<div className={`popout-menu ${menuClass}`}>
					<ul>
						<li>Settings</li>
						<li onClick={this.logout.bind(this)}>Logout</li>
					</ul>
				</div>
			</div>
		)
	}
}

// This page uses the router history. In order to gain access to the history, the class needs
// to be exported wrapped by the router. Now inside of LogoutButtonInternal, this.props will have a history object
export const UserButton = withRouter(LogoutButtonInternal);
