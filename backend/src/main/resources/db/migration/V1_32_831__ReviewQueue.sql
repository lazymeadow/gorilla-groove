ALTER TABLE track_history ADD listened_in_review bit(1) DEFAULT b'0' NOT NULL;
ALTER TABLE track ADD review_source_id int REFERENCES review_source(id);
ALTER TABLE track ADD last_reviewed timestamp;
ALTER TABLE track ADD in_review bit DEFAULT b'0' NOT NULL;

CREATE TABLE review_source
(
    id int PRIMARY KEY NOT NULL AUTO_INCREMENT
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
	track_length int not null,
	track_release_year int not null,
	track_art_url varchar(512) null,
	discovered_at timestamp default CURRENT_TIMESTAMP not null,
	last_download_attempt timestamp null,
	downloaded_at timestamp null
);


