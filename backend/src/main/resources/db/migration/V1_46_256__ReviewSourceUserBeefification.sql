ALTER TABLE review_source_user ADD offline_availability TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE review_source_user ADD deleted BIT DEFAULT b'0' NOT NULL;
ALTER TABLE review_source_user ADD updated_at TIMESTAMP DEFAULT NOW() NOT NULL;
ALTER TABLE review_source_user ADD created_at TIMESTAMP DEFAULT NOW() NOT NULL;
ALTER TABLE review_source_user ADD id int NOT NULL PRIMARY KEY AUTO_INCREMENT;
ALTER TABLE review_source_user
  MODIFY COLUMN id int NOT NULL auto_increment FIRST;

ALTER TABLE review_source DROP deleted;

CREATE UNIQUE INDEX review_source_artist_download_review_source_id_track_name_uindex ON review_source_artist_download (review_source_id, track_name);