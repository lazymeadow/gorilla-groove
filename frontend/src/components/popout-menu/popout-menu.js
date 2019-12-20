import React from 'react';

export class PopoutMenu extends React.Component {
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

	toggleExpanded(event) {
		event.stopPropagation();
		this.setState({expanded: !this.state.expanded});
	}

	render() {
		let menuClass = this.state.expanded ? '' : 'hidden';
		return (
			<div>
				<div onClick={(e) => this.toggleExpanded(e)} className={this.props.mainItem.className}>
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
									return <li key={index} onClick={menuItem.clickHandler}>{menuItem.text}</li>
								}
							})}
					</ul>
				</div>
			</div>
		)
	}
}
