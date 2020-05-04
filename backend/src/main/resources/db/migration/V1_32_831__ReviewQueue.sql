ALTER TABLE track_history ADD listened_in_review bit(1) DEFAULT b'0' NOT NULL;

CREATE TABLE review_source
(
    id int PRIMARY KEY NOT NULL AUTO_INCREMENT,
    review_source_type smallint NOT NULL,
    review_source_implementation_id int NOT NULL -- The table this references depends on the source_type, so no FK can be used
);

CREATE TABLE review_source_youtube_channel
(
    id int PRIMARY KEY NOT NULL AUTO_INCREMENT,
    channel_id varchar(255) NOT NULL,
    channel_name varchar(255) NOT NULL,
    last_searched TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE review_source_user
(
    id int PRIMARY KEY NOT NULL AUTO_INCREMENT,
    user_id int NOT NULL REFERENCES `user`(id)
);

CREATE TABLE track_review_info
(
    id int PRIMARY KEY NOT NULL AUTO_INCREMENT,
    track_id int NOT NULL REFERENCES track(id),
    review_source_id int NOT NULL REFERENCES review_source(id),
    last_reviewed timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE review_source_user
(
    review_source_id int NOT NULL REFERENCES review_source(id),
    user_id int NOT NULL REFERENCES `user`(id)
);
