ALTER TABLE track ADD filesize_song_mp3 INT unsigned DEFAULT 0 NOT NULL;
ALTER TABLE track ADD filesize_song_ogg INT unsigned DEFAULT 0 NOT NULL;
ALTER TABLE track ADD filesize_art_png MEDIUMINT unsigned DEFAULT 0 NOT NULL;
ALTER TABLE track ADD filesize_thumbnail_64x64_png MEDIUMINT unsigned DEFAULT 0 NOT NULL;

ALTER TABLE track DROP bit_rate;
ALTER TABLE track DROP sample_rate;
