ALTER TABLE review_source ADD updated_at timestamp DEFAULT current_timestamp NOT NULL;
ALTER TABLE review_source ADD deleted bit DEFAULT b'0' NOT NULL;

UPDATE review_source SET updated_at = created_at;
