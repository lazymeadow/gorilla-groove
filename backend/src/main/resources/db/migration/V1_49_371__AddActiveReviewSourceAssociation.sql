ALTER TABLE review_source_user ADD active bit(1) DEFAULT b'1' NULL;
ALTER TABLE review_source_user
  MODIFY COLUMN active bit(1) DEFAULT b'1' AFTER deleted;
