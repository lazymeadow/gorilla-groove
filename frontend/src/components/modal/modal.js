import React from 'react';
import ReactModal from 'react-modal';

const customStyles = {
	content : {
		top                   : '50%',
		left                  : '50%',
		right                 : 'auto',
		bottom                : 'auto',
		marginRight           : '-50%',
		transform             : 'translate(-50%, -50%)'
	}
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
				onRequestClose={(e) => this.closeModal(e)}
				style={customStyles}
				contentLabel="Example Modal"
			>
				<span className="modal-close" onMouseDown={this.closeModal.bind(this)}>✗</span>
				<div onKeyDown={this.stopPropagation.bind(this)}>
					{this.props.children}
				</div>
			</ReactModal>
		);
	}
}