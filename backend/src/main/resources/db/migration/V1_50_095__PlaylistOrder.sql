ALTER TABLE playlist_track ADD `sort_order` SMALLINT UNSIGNED DEFAULT 0 NOT NULL;

CREATE INDEX playlist_track__sort_order ON playlist_track (sort_order);

-- Previously, the deleted flag on playlist_tracks was not used for anything.
-- I'd like to stop having to do joins to see if a playlist_track is valid. Makes
-- sense that when we deleted a track, we delete all playlist_tracks that used it.
-- So retroactively update all playlist_tracks that ought to be deleted.
UPDATE playlist_track pt
JOIN track t ON (t.id = pt.track_id)
SET pt.deleted = 1 WHERE t.deleted = 1
