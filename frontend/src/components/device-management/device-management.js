import React, {useContext, useEffect, useState} from 'react';
import {Modal} from "../modal/modal";
import {Api} from "../../api";
import {toast} from "react-toastify";
import {toTitleCaseFromSnakeCase} from "../../formatters";
import {LoadingSpinner} from "../loading-spinner/loading-spinner";
import {DeviceContext} from "../../services/device-provider";

function DeviceManagementModal() {
	const [devices, setDevices] = useState([]);

	const [clickedDevice, setClickedDevice] = useState(null);

	const [editOpen, setEditOpen] = useState(false);
	const [archiveOpen, setArchiveOpen] = useState(false);
	const [mergeOpen, setMergeOpen] = useState(false);
	const [deleteOpen, setDeleteOpen] = useState(false);

	const deviceContext = useContext(DeviceContext);

	useEffect(() => {
		Api.get('device').then(setDevices);
	}, []);

	const changeDeviceName = newName => {
		return Api.put(`device/update/${clickedDevice.id}`, { deviceName: newName }).then(() => {
			clickedDevice.deviceName = newName;

			if (clickedDevice.id === deviceContext.ownDevice.id) {
				deviceContext.ownDevice.deviceName = newName;
			}

			setDevices(devices); // Force re-render
			toast.success('Name changed successfully');
		}).catch(err => {
			console.error(err);
			toast.error('Failed to update the device name');
		});
	};

	const archiveDevice = () => {
		return Api.put(`device/archive/${clickedDevice.id}`, { archived: true }).then(() => {
			if (clickedDevice.id === deviceContext.ownDevice.id) {
				deviceContext.ownDevice.archived = true;
			}

			setDevices(devices.filter(device => device.id !== clickedDevice.id));
			toast.success('Device archived successfully');
		}).catch(err => {
			console.error(err);
			toast.error('Failed to archive the device');
		});
	};

	const mergeDevice = targetId => {
		return Api.put(`device/merge`, {
			id: clickedDevice.id,
			targetId
		}).then(() => {
			if (clickedDevice.id === deviceContext.ownDevice.id) {
				deviceContext.loadOwnDevice();
			}

			setDevices(devices.filter(device => device.id !== clickedDevice.id));
			toast.success('Device merged successfully');
		}).catch(err => {
			console.error(err);
			toast.error('Failed to merge the device');
		});
	};

	const deleteDevice = () => {
		if (clickedDevice.id === deviceContext.ownDevice.id) {
			toast.info("You mustn't delete your active device! The world would surely break.");
			return Promise.resolve();
		}

		return Api.delete(`device/${clickedDevice.id}`).then(() => {
			setDevices(devices.filter(device => device.id !== clickedDevice.id));
			toast.success('Device deleted successfully');
		}).catch(err => {
			console.error(err);
			toast.error('Failed to delete the device');
		});
	};

	const currentId = deviceContext.ownDevice === null ? -1 : deviceContext.ownDevice.id;
	const currentDeviceName = deviceContext.ownDevice === null ? '' : deviceContext.ownDevice.deviceName;

	return (
		<div onKeyDown={e => e.nativeEvent.propagationStopped = true}>
			<div id="device-management-modal" className="p-relative">
				<h2 className="text-center">Device Management</h2>

				<LoadingSpinner visible={devices.length === 0 || deviceContext.ownDevice === null}/>

				<section id="current-device">
					You are currently using: <strong>{currentDeviceName}</strong>
				</section>

				<table className="data-table">
					<thead>
					<tr>
						<th>Name</th>
						<th>Type</th>
						<th>Last Version</th>
						<th>Last IP</th>
						<th/>
					</tr>
					</thead>
					<tbody>
					{devices.map(device => (
						<tr key={device.id} className={device.id === currentId ? 'bold' : ''}>
							<td>{device.deviceName}</td>
							<td>{toTitleCaseFromSnakeCase(device.deviceType)}</td>
							<td>{device.applicationVersion}</td>
							<td>{device.lastIp}</td>
							<td className="device-actions">
								<i className="fas fa-edit" title="Edit name" onClick={() => {
									setClickedDevice(device);
									setEditOpen(true);
								}}/>
								<i className="fas fa-archive" title="Archive device" onClick={() => {
									setClickedDevice(device);
									setArchiveOpen(true);
								}}/>
								<i className="fas fa-compress-arrows-alt" title="Merge devices" onClick={() => {
									setClickedDevice(device);
									setMergeOpen(true);
								}}/>
								<i className="fas fa-times" title="Delete device" onClick={() => {
									setClickedDevice(device);
									setDeleteOpen(true);
								}}/>
							</td>
						</tr>
					))}
					</tbody>
				</table>

				{ clickedDevice === null
					? <div/>
					: (
						<div>
							<Modal
								isOpen={editOpen}
								closeFunction={() => setEditOpen(false)}
							>
								<form className="form-modal" onSubmit={e => {
									e.preventDefault();
									const newName = document.getElementById('edit-device-name').value;
									changeDeviceName(newName).then(() => setEditOpen(false))
								}}>
									<h3>Edit Name</h3>
									<input id="edit-device-name" className="long-property" defaultValue={clickedDevice.deviceName}/>
									<div className="flex-between confirm-modal-buttons">
										<button type="submit">Update</button>
										<button type="button" onClick={() => setEditOpen(false)}>Cancel</button>
									</div>
								</form>
							</Modal>

							<Modal
								isOpen={archiveOpen}
								closeFunction={() => setArchiveOpen(false)}
							>
								<form className="form-modal" onSubmit={e => {
									e.preventDefault();
									archiveDevice().then(() => setArchiveOpen(false))
								}}>
									<h3>Archive</h3>
									<div className="additional-device-action-info">
										Archiving a device will prevent it from showing up on this screen.<br/>
										Use it to hide devices that you want to keep the history from, but are no longer relevant.
									</div>
									<div className="flex-between confirm-modal-buttons">
										<button type="submit">Make it so</button>
										<button type="button" onClick={() => setArchiveOpen(false)}>Cancel</button>
									</div>
								</form>
							</Modal>

							<Modal
								isOpen={mergeOpen}
								closeFunction={() => setMergeOpen(false)}
							>
								<form className="form-modal" onSubmit={e => {
									e.preventDefault();
									const selectEl = document.getElementById('device-merge-select');
									const targetId = selectEl.options[selectEl.selectedIndex].value;
									mergeDevice(parseInt(targetId)).then(() => setMergeOpen(false))
								}}>
									<h3>Merge Devices</h3>
									<div className="additional-device-action-info">
										Merging this device into another device will effectively delete it.<br/>
										All prior and future song listens will use the selected device.<br/>
										This action is not reversible.<br/><br/>
										Use this to link multiple browsers on the same computer,<br/>
										or to keep your phone history on the same device after a factory reset.<br/>
									</div>

									<hr/>
									<label htmlFor="device-merge-select" className="d-block">
										Merge {clickedDevice.deviceName} into:
									</label>
									<select id="device-merge-select">
										{ devices
											.filter(device => device.id !== clickedDevice.id && device.deviceType === clickedDevice.deviceType)
											.map(device => (
												<option value={device.id} key={device.id}>
													{ device.deviceName }
												</option>
											))}
									</select>
									<div className="flex-between confirm-modal-buttons">
										<button type="submit">FU-SHUN HA!</button>
										<button type="button" onClick={() => setMergeOpen(false)}>Cancel</button>
									</div>
								</form>
							</Modal>

							<Modal
								isOpen={deleteOpen}
								closeFunction={() => setDeleteOpen(false)}
							>
								<form className="form-modal" onSubmit={e => {
									e.preventDefault();
									deleteDevice().then(() => setDeleteOpen(false))
								}}>
									<h3>Delete</h3>
									<div className="additional-device-action-info">
										Deleting a device will unlink it from history data, but the history data will survive.<br/>
										Consider merging the device to an existing one to keep a device on the history data.<br/>
										This action is not reversible.
									</div>
									<div className="flex-between confirm-modal-buttons">
										<button type="submit">Don't question me, form</button>
										<button type="button" onClick={() => setDeleteOpen(false)}>Cancel</button>
									</div>
								</form>
							</Modal>

						</div>
					)
				}
			</div>
		</div>
	)
}

export default function DeviceManagement() {
	const [modalOpen, setModalOpen] = useState(false);

	const closeFunction = () => setModalOpen(false);

	return (
		<div id="device-management" onClick={() => setModalOpen(true)}>
			Devices
			<Modal
				isOpen={modalOpen}
				closeFunction={closeFunction}
			>
				{ modalOpen ? <DeviceManagementModal closeFunction={closeFunction}/> : <div/> }
			</Modal>
		</div>
	)
}
