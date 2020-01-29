import React from 'react';

export class PopoutMenu extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			expanded: false
		};

		// Bind the function once so it is able to be removed in the unmount
		this.boundCloseMenu = this.closeMenu.bind(this);
	}

	componentDidMount() {
		document.body.addEventListener('mousedown', this.boundCloseMenu);
	}

	componentWillUnmount() {
		document.body.removeEventListener('mousedown', this.boundCloseMenu);
	}

	closeMenu() {
		this.setState({ expanded: false });
	}

	toggleExpanded(event) {
		event.stopPropagation();
		this.setState({ expanded: !this.state.expanded });
	}

	render() {
		let menuClass = this.state.expanded ? '' : 'hidden';
		return (
			<div>
				<div onMouseDown={this.toggleExpanded.bind(this)} className={this.props.mainItem.className}>
					{this.props.mainItem.text}
				</div>
				<div className={`popout-menu ${menuClass}`}>
					<ul>
						{this.props.menuItems
							.filter(menuItem => menuItem.shouldRender === undefined || menuItem.shouldRender === true)
							.map((menuItem, index) => {
								if (menuItem.component) {
									return <li key={index}>{menuItem.component}</li>
								} else {
									return <li key={index} onMouseDown={menuItem.clickHandler}>{menuItem.text}</li>
								}
							})}
					</ul>
				</div>
			</div>
		)
	}
}
