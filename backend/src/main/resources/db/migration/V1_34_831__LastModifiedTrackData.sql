ALTER TABLE track ADD song_updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE track ADD art_updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL;

UPDATE track set song_updated_at = updated_at, art_updated_at = updated_at;

