import React, {useContext, useEffect, useState} from 'react';
import TreeView from 'react-treeview'
import {MusicContext} from "../../services/music-provider";
import {TrackView} from "../../enums/track-view";
import {EditableDiv} from "../editable-div/editable-div";
import {AddPlaylistButton} from "../add-playlist/add-playlist";
import {Modal} from "../modal/modal";
import {SocketContext} from "../../services/socket-provider";
import {UserContext} from "../../services/user-provider";

let pendingDeletePlaylist = {};

export default function TrackSourceList(props) {
	const [collapsedBookkeeping, setCollapsedBookkeeping] = useState([false, false, false]);
	const [editedId, setEditedId] = useState(null);
	const [modalOpen, setModalOpen] = useState(false);

	const userContext = useContext(UserContext);
	const musicContext = useContext(MusicContext);
	const socketContext = useContext(SocketContext);

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

		if (section === TrackView.USER) {
			musicContext.loadSongsForUser(entry.id, {}, false);
		} else {
			musicContext.loadSongsForPlaylist(entry.id, {}, false);
		}
	};

	const getNowPlayingElement = (entry) => {
		const song = socketContext.nowListeningUsers[entry.email];
		if (!song || !song.trackId) {
			return <span/>
		}

		const artist = song.trackArtist ? song.trackArtist : 'Unknown';
		const name = song.trackName ? song.trackName : 'Unknown';

		return <span className="user-listening" title={`${artist} - ${name}`}>♬</span>
	};

	const librarySelected = musicContext.trackView === TrackView.LIBRARY ? 'selected' : '';

	return (
		<div id="view-source-list">
			<div
				className={`library-option ${librarySelected}`}
				onMouseDown={() => musicContext.loadSongsForUser()}
			>
				<span className="my-library">My Library</span>
			</div>
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
										updateHandler={newValue => musicContext.renamePlaylist(entry, newValue)}
									/>
									{ getNowPlayingElement(entry) }

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
							musicContext.deletePlaylist(pendingDeletePlaylist).then(() => setModalOpen(false))
						}}>You know I do</button>
						<button onMouseDown={() => setModalOpen(true)}>No. Woops</button>
					</div>
				</div>
			</Modal>

		</div>
	);
}
