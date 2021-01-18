import React from "react";
import {Api} from "../api";
import {toast} from "react-toastify";

export const BackgroundTaskContext = React.createContext();

export class BackgroundTaskProvider extends React.Component {
	constructor(props) {
		super(props);

		this.state = {
			unfinishedTasks: [],
			finishedTasks: [],

			reloadBackgroundTasks: (...args) => this.reloadBackgroundTasks(...args),
			handleTaskChange: (...args) => this.handleTaskChange(...args),
		}
	}

	handleTaskChange(task, trackId, musicContext) {
		let newUnfinishedTasks = this.state.unfinishedTasks.slice(0);
		let newFinishedTasks = this.state.finishedTasks.slice(0);

		newUnfinishedTasks = newUnfinishedTasks.filter(it => it.id !== task.id);

		if (task.status === BackgroundTaskStatus.PENDING) {
			newUnfinishedTasks.push(task);
		} else if (task.status === BackgroundTaskStatus.RUNNING) {
			newUnfinishedTasks.push(task);
		} else if (task.status === BackgroundTaskStatus.COMPLETE) {
			newFinishedTasks.push(task);

			Api.get(`track/${trackId}`).then(track => {
				musicContext.addUploadToExistingLibraryView(track);
				toast.success(`'${task.description}' was downloaded successfully`)
			});
		} else if (task.status === BackgroundTaskStatus.FAILED) {
			newFinishedTasks.push(task);
			toast.error(`Failed to download video: '${task.description}'`)
		}

		this.setState({
			unfinishedTasks: newUnfinishedTasks,
			finishedTasks: newFinishedTasks
		});
	}

	reloadBackgroundTasks() {
		Api.get('background-task/unfinished').then(res => {
			this.setState({ tasks: res.items });
		}).catch(err => {
			console.log(err);
		});
	}

	render() {
		return (
			<BackgroundTaskContext.Provider value={this.state}>
				{this.props.children}
			</BackgroundTaskContext.Provider>
		)
	}
}

const BackgroundTaskStatus = Object.freeze({
	NONE: 'NONE',
	PENDING: 'PENDING',
	RUNNING: 'RUNNING',
	FAILED: 'FAILED',
	COMPLETE: 'COMPLETE',
});
