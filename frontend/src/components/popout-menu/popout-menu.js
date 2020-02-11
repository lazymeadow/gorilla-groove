import React, {useEffect, useState} from 'react';

export default function PopoutMenu(props) {
	const [expanded, setExpanded] = useState(false);

	const closeMenu = () => {
		setExpanded(false);
	};

	const toggleExpanded = event => {
		event.stopPropagation();
		setExpanded(!expanded);
	};

	useEffect(() => {
		// If something using this view has its own view on how to handle expansion, don't handle it internally here
		if (props.expansionOverride !== undefined) {
			return;
		}

		if (expanded) {
			setTimeout(() => {
				document.body.addEventListener('mousedown', closeMenu);
			});
		} else {
			document.body.removeEventListener('mousedown', closeMenu);
		}
	}, [expanded]);

	let menuClass = props.expansionOverride || expanded ? '' : 'hidden';

	return (
		<div>
			{ props.mainItem ?
				<div onMouseDown={toggleExpanded} className={props.mainItem.className}>
					{props.mainItem.text}
				</div> : null
			}

			<div className={`popout-menu ${menuClass}`}>
				<ul>
					{props.menuItems
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
