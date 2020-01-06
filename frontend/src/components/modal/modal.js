import React from 'react';
import ReactModal from 'react-modal';

const customStyles = {
	content : {
		top: '50%',
		left: '50%',
		right: 'auto',
		bottom: 'auto',
		padding: '20px',
		transform: 'translate(-50%, -50%)'
	}
};

const fullScreenOverrides = {
	padding: '0',
	width: '100%',
	height: '100%'
};

const fullScreenStyles = {
	content: Object.assign({}, customStyles.content, fullScreenOverrides)
};

// Make sure to bind modal to your appElement (http://reactcommunity.org/react-modal/accessibility/)
ReactModal.setAppElement('#root');

export class Modal extends React.Component {
	constructor(props) {
		super(props);

		this.afterOpenModal = this.afterOpenModal.bind(this);
	}

	afterOpenModal() {
		// references are now sync'd and can be accessed.
		// this.subtitle.style.color = '#f00';
	}

	closeModal(event) {
		event.stopPropagation();
		this.props.closeFunction();
	}

	// When we push enter, a submit event is fired and a key event
	// Wrap the entire form in a key listener that stops the propagation of the key event so songs don't play from 'enter'
	stopPropagation(event) {
		event.nativeEvent.propagationStopped = true;
	}

	render() {
		return (
			<ReactModal
				isOpen={this.props.isOpen}
				// onAfterOpen={this.afterOpenModal}
				onRequestClose={this.closeModal.bind(this)}
				style={this.props.fullScreen ? fullScreenStyles : customStyles}
				contentLabel="Example Modal"
			>
				<span className="modal-close" onClick={this.closeModal.bind(this)}>✗</span>
				<div onKeyDown={this.stopPropagation.bind(this)}>
					{this.props.children}
				</div>
			</ReactModal>
		);
	}
}
