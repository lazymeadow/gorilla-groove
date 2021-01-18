import React, {useContext, useEffect, Fragment} from 'react';
import {BackgroundTaskContext} from "../../services/background-task-provider";

export default function BackgroundTaskProgress() {
	const taskContext = useContext(BackgroundTaskContext);

	const total = taskContext.finishedTasks.length + taskContext.unfinishedTasks.length;

	let percent = 0;
	if (total > 0) {
		percent = parseInt(taskContext.finishedTasks.length / total * 100)
	}

	useEffect(() => {
		taskContext.reloadBackgroundTasks();
	}, []);

	const floatingWindowClass = total > 0 ? '' : 'display-none';

	const allTasks = taskContext.finishedTasks.concat(taskContext.unfinishedTasks);

	return (
		<div id="background-task-progress">
			{ taskContext.unfinishedTasks.length > 0
				? <>
					<progress className="overall-upload-progress" value={percent} max="100"/>
					<div className="count-overlay">
						Processing {taskContext.finishedTasks.length + 1} of {total}
					</div>

					<div className={`floating-window ${floatingWindowClass}`}>
						<div className="progress-grid">
							<div className="grid-head">Download</div>
							<div className="grid-head">Status</div>
							{ allTasks.map(task =>
								<Fragment key={task.id}>
									<div className="grid-item">{task.description}</div>
									<div className="grid-item">{task.status}</div>
								</Fragment>
							)}
						</div>
					</div>
				</>
				: null
			}
		</div>
	)
}
