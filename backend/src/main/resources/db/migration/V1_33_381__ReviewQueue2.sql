CREATE UNIQUE INDEX review_source_user_review_source_id_user_id_uindex ON review_source_user (review_source_id, user_id);

ALTER TABLE track ADD original_track_id int NULL;
