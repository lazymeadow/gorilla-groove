-- These are all used to find the latest updated entry when syncing
CREATE INDEX playlist_track_playlist_id_updated_at_index ON playlist_track (playlist_id, updated_at);

CREATE INDEX playlist_id_updated_at_index ON playlist (id, updated_at);

CREATE INDEX review_source_id_updated_at_index ON review_source (id, updated_at);

CREATE INDEX track_user_id_updated_at_index ON track (user_id, updated_at);

CREATE INDEX user_updated_at_index ON user (updated_at);
