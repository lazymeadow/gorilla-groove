import React, {useContext, useState, useEffect} from 'react';
import {Modal} from "../modal/modal";
import {toast} from "react-toastify";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {Api} from "../../api";
import {UserContext} from "../../services/user-provider";
import {BackgroundTaskContext} from "../../services/background-task-provider";


export default function BackgroundTaskProgress() {
	const taskContext = useContext(BackgroundTaskContext);

	const total = taskContext.finishedTasks.length + taskContext.unfinishedTasks.length;

	let percent = 0;
	if (total > 0) {
		percent = parseInt(taskContext.unfinishedTasks.length / total * 100)
	}

	useEffect(() => {
		taskContext.reloadBackgroundTasks();
	}, []);

	return (
		<div id="background-task-progress">
			<progress className="overall-upload-progress" value={percent} max="100"/>
			<div className="count-overlay">
				{taskContext.finishedTasks.length} / {total}
			</div>
		</div>
	)
}
