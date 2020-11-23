import React, {useEffect, useState} from 'react';
import {Modal} from "../modal/modal";
import {Api} from "../../api";
import {toast} from "react-toastify";
import {formatDate} from "../../formatters";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";

function CrashReportModal() {
	const [crashReports, setCrashReports] = useState([]);
	const [selectedReport, setSelectedReport] = useState({});
	const [crashLog, setCrashLog] = useState("");

	useEffect(() => {
		Api.get('crash-report').then(crashReports => {
			setCrashReports(crashReports);
			if (crashReports.length > 0) {
				setActiveCrashReport(crashReports[0]);
			}
		});
	}, []);

	const getCrashLog = id => {
		Api.get(`crash-report/${id}/log`).then(res => {
			setCrashLog(res.deviceLog);
		}).catch(e => {
			console.error(e);
			toast.error('Unable to load crash log');
		})
	};

	const setActiveCrashReport = crashReport => {
		setSelectedReport(crashReport);
		setCrashLog("");
		getCrashLog(crashReport.id);
	};

	const downloadCrashDb = id => {
		Api.download(`crash-report/${id}/db`);
	};

	const deleteCrashReport = id => {
		Api.delete(`crash-report/${id}`).then(() => {
			toast.success('Crash report deleted successfully');
			const newReports = crashReports.slice(0).filter(it => it.id !== id);
			setCrashReports(newReports);

			const activeCrashReport = newReports.length > 0 ? newReports[0] : {};
			setActiveCrashReport(activeCrashReport);
		}).catch(() => {
			toast.error('Failed to delete crash report');
		});
	};

	return (
		<div onKeyDown={e => e.nativeEvent.propagationStopped = true}>
			<div id="crash-report-modal" className="p-relative">
				<h2 className="text-center">Crash Reports</h2>

				<LoadingSpinner visible={false}/>

				<div id="table-container">
					<ul className="table-list">
						{crashReports.map(crashReport => (
							<li
								key={crashReport.id}
								className={crashReport.id === selectedReport.id ? 'selected' : ''}
								onClick={() => setActiveCrashReport(crashReport)}
							>
								{crashReport.deviceOwner} ({formatDate(crashReport.createdAt)})
							</li>
						))}
					</ul>
					<div className="table-content">
						<div className="crash-report-info">
							<div className="d-flex">
								<span className="header-item small-item">
									<span className="header-key">ID: </span>
									<span className="header-value">{selectedReport.id}</span>
								</span>
								<span className="header-item large-item">
									<span className="header-key">User: </span>
									<span className="header-value">{selectedReport.deviceOwner}</span>
								</span>
								<span className="header-item">
									<span className="header-key">Device: </span>
									<span className="header-value">{selectedReport.deviceType}</span>
								</span>
								<span className="header-item text-right">
									<button onClick={() => downloadCrashDb(selectedReport.id)}>Download DB</button>
								</span>
							</div>
							<div className="d-flex">
								<span className="header-item small-item">
									<span className="header-key">Size: </span>
									<span className="header-value">{selectedReport.sizeKb}KB</span>
								</span>
								<span className="header-item large-item">
									<span className="header-key">Reported: </span>
									<span className="header-value">{formatDate(selectedReport.createdAt, true)}</span>
								</span>
								<span className="header-item">
									<span className="header-key">Version: </span>
									<span className="header-value">{selectedReport.version}</span>
								</span>
								<span className="header-item text-right">
									<button onClick={() => deleteCrashReport(selectedReport.id)}>Delete</button>
								</span>
							</div>
						</div>

						<textarea id="report-log" value={crashLog} readOnly={true}/>
					</div>
				</div>
			</div>
		</div>
	)
}

export default function CrashReport() {
	const [modalOpen, setModalOpen] = useState(false);

	const closeFunction = () => setModalOpen(false);

	return (
		<div id="crash-report" onClick={() => setModalOpen(true)}>
			Crash Reports
			<Modal
				isOpen={modalOpen}
				closeFunction={closeFunction}
			>
				{ modalOpen ? <CrashReportModal closeFunction={closeFunction}/> : null }
			</Modal>
		</div>
	)
}
