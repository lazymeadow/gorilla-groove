ALTER TABLE track_history ADD listened_in_review bit(1) DEFAULT b'0' NOT NULL;
ALTER TABLE track ADD review_source_id int REFERENCES review_source(id);
ALTER TABLE track ADD last_reviewed timestamp;
ALTER TABLE track ADD added_to_library timestamp AFTER last_played;
ALTER TABLE track ADD in_review bit DEFAULT b'0' NOT NULL;

-- Up until now, the day that stuff got added to the library was the same that they were created.
-- Going forward, things added via review_queue will have a different created and added date
UPDATE track SET added_to_library = created_at;

CREATE TABLE review_source
(
    id int PRIMARY KEY NOT NULL AUTO_INCREMENT,
    source_type int NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE review_source_user
(
    review_source_id INT NOT NULL REFERENCES review_source(id),
    user_id int NOT NULL REFERENCES `user`(id)
);

CREATE TABLE review_source_youtube_channel
(
    review_source_id INT PRIMARY KEY REFERENCES review_source(id),
    channel_id varchar(255) NOT NULL,
    channel_name varchar(255) NOT NULL,
    last_searched TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX review_source_youtube_channel_channel_id_uindex ON review_source_youtube_channel (channel_id);

CREATE TABLE review_source_user_recommend
(
    review_source_id INT PRIMARY KEY REFERENCES review_source(id),
    user_id INT NOT NULL REFERENCES `user`(id)
);

CREATE TABLE review_source_artist
(
    review_source_id INT PRIMARY KEY REFERENCES review_source(id),
    artist_id VARCHAR(255) NOT NULL,
    artist_name VARCHAR(255) NOT NULL,
    search_newer_than TIMESTAMP NULL
);

create table review_source_artist_download
(
	id int not null primary key AUTO_INCREMENT,
	review_source_id int null,
	track_name varchar(255) not null,
	track_album_name varchar(255) not null,
	track_length int not null,
	track_release_year int not null,
	track_art_url varchar(512) null,
	discovered_at timestamp default CURRENT_TIMESTAMP not null,
	last_download_attempt timestamp null,
	downloaded_at timestamp null
);

CREATE UNIQUE INDEX review_source_artist_artist_name_uindex ON review_source_artist (artist_name);

ALTER TABLE track_link DROP FOREIGN KEY FK6ht8dsdjj5btmsi968hehjxit;
ALTER TABLE track_link
ADD CONSTRAINT FK6ht8dsdjj5btmsi968hehjxit
FOREIGN KEY (track_id) REFERENCES track (id) ON DELETE CASCADE;
