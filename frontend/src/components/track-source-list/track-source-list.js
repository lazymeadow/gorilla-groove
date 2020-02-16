import React, {useContext, useEffect, useState} from 'react';
import TreeView from 'react-treeview'
import {MusicContext} from "../../services/music-provider";
import {CenterView, TrackView} from "../../enums/site-views";
import {EditableDiv} from "../editable-div/editable-div";
import AddPlaylistButton from "../add-playlist/add-playlist";
import {Modal} from "../modal/modal";
import {SocketContext} from "../../services/socket-provider";
import {UserContext} from "../../services/user-provider";
import {PlaylistContext} from "../../services/playlist-provider";
import {PermissionType} from "../../enums/permission-type";

let pendingDeletePlaylist = {};

export default function TrackSourceList(props) {
	const [collapsedBookkeeping, setCollapsedBookkeeping] = useState([false, false, false]);
	const [editedId, setEditedId] = useState(null);
	const [modalOpen, setModalOpen] = useState(false);

	const userContext = useContext(UserContext);
	const musicContext = useContext(MusicContext);
	const socketContext = useContext(SocketContext);
	const playlistContext = useContext(PlaylistContext);

	const dataSource = [
		{
			section: TrackView.USER,
			heading: 'User Libraries',
			data: userContext.otherUsers
		}, {
			section: TrackView.PLAYLIST,
			heading: <span className="playlist-heading">Playlists <AddPlaylistButton/></span>,
			data: props.playlists
		}
	];

	const handleEditStop = event => {
		if (
			event.target.tagName !== 'INPUT'
			&& event.target.id !== editedId
			&& editedId !== null
		) {
			setEditedId(null);
		}
	};

	useEffect(() => {
		document.body.addEventListener('click', handleEditStop);

		return () => {
			document.body.removeEventListener('click', handleEditStop);
		}
	}, [editedId]);

	const handleParentNodeClick = i => {
		const [...bookkeeping] = collapsedBookkeeping;
		bookkeeping[i] = !bookkeeping[i];
		setCollapsedBookkeeping(bookkeeping);
	};

	const selectEntry = (section, entry, elementId) => {
		if (section === musicContext.trackView && entry.id === musicContext.viewedEntityId) {
			setEditedId(elementId);
			return; // User selected the same thing as before
		}
		setEditedId(null);

		props.setCenterView(CenterView.TRACKS);
		if (section === TrackView.USER) {
			musicContext.loadSongsForUser(entry.id, {}, false);
		} else {
			musicContext.loadSongsForPlaylist(entry.id, {}, false);
		}
	};

	const getNowPlayingElement = entry => {
		const listeningDevices = socketContext.nowListeningUsers[entry.id];
		if (!listeningDevices) {
			return <span/>
		}

		const isMobile = listeningDevices.every(device => device.isMobile);

		const displayText = listeningDevices.map(device =>
			device.song + '\nDevice: ' + device.deviceName
		).join('\n\n');

		return <span className="user-listening" title={displayText}>
			{ isMobile ? <i className="fas fa-mobile"/> : <i className="fas fa-music"/> }
		</span>
	};

	const librarySelected = musicContext.trackView === TrackView.LIBRARY && props.centerView === CenterView.TRACKS
		? 'selected' : '';
	const deviceManagementSelected = props.centerView === CenterView.REMOTE_DEVICES
		? 'selected' : '';

	return (
		<div id="view-source-list">
			<div
				className={`large-option ${librarySelected}`}
				onMouseDown={() => {
					props.setCenterView(CenterView.TRACKS);
					musicContext.loadSongsForUser();
				}}
			>
				<span className="my-library">My Library</span>
			</div>

			{ userContext.hasPermission(PermissionType.EXPERIMENTAL) ?
				<div
					className={`large-option ${deviceManagementSelected}`}
					onMouseDown={() => {
						props.setCenterView(CenterView.REMOTE_DEVICES);
					}}
				>
					<span className="remote-play">Remote Play</span>
				</div>
				: null
			}

			{dataSource.map((node, i) => {
				const label =
					<div className="tree-node" onMouseDown={() => handleParentNodeClick(i)}>
						{node.heading}
					</div>;
				return (
					<TreeView
						key={i}
						nodeLabel={label}
						collapsed={collapsedBookkeeping[i]}
						onClick={() => handleParentNodeClick(i)}
					>
						{node.data.map(entry => {
							const entrySelected = musicContext.trackView === node.section & entry.id === musicContext.viewedEntityId;
							const entryClass = entrySelected ? 'selected' : '';
							const cellId = i + '-' + entry.id;
							return (
								<div
									id={cellId}
									className={`tree-child ${entryClass}`}
									key={entry.id}
									onMouseDown={() => selectEntry(node.section, entry, cellId)}
								>
									<EditableDiv
										editable={editedId === cellId && node.section === TrackView.PLAYLIST}
										text={entry.username ? entry.username : entry.name}
										stopEdit={() => setEditedId(null)}
										updateHandler={newValue => playlistContext.renamePlaylist(entry, newValue)}
									/>
									{ node.section === TrackView.USER ? getNowPlayingElement(entry) : <React.Fragment/> }

									<div className="playlist-delete">
										{ node.section === TrackView.PLAYLIST
											? <i className="fas fa-times" onMouseDown={e => {
												e.stopPropagation();
												pendingDeletePlaylist = entry;
												setModalOpen(true);
											}}/>
											: <i/>
										}
									</div>

								</div>
							)
						})}
					</TreeView>
				);
			})}
			<Modal
				isOpen={modalOpen}
				closeFunction={() => setModalOpen(false)}
			>
				<div id="playlist-delete-modal">
					<div>Are you sure you want to delete the playlist '{pendingDeletePlaylist.name}'?</div>
					<div className="flex-between confirm-modal-buttons">
						<button onMouseDown={() => {
							playlistContext.deletePlaylist(pendingDeletePlaylist).then(() => {
								if (musicContext.trackView === TrackView.PLAYLIST
									&& musicContext.viewedEntityId === pendingDeletePlaylist.id) {
									musicContext.loadSongsForUser();
								}
								setModalOpen(false)
							})
						}}>You know I do</button>
						<button onMouseDown={() => setModalOpen(false)}>No. Woops</button>
					</div>
				</div>
			</Modal>

		</div>
	);
}


