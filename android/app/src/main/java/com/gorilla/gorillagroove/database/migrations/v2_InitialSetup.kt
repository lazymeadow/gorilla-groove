package com.gorilla.gorillagroove.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gorilla.gorillagroove.database.execMultipleSQL
import com.gorilla.gorillagroove.service.GGLog.logInfo

val MIGRATION_1_2 = object : Migration(1, 2) {
	override fun migrate(database: SupportSQLiteDatabase) {
        logInfo("Migrating database to version 2")

		database.execMultipleSQL(
			"""
create table user (
	id INT not null primary key unique,
	name TEXT not null,
	last_login INT,
	created_at INT not null
);

create table track (
	id INTEGER not null primary key unique,
	name TEXT not null,
	artist TEXT not null,
	featuring TEXT not null,
	album TEXT not null,
	track_number INTEGER,
	length INTEGER not null,
	release_year INTEGER,
	genre TEXT,
	play_count INTEGER not null,
	is_private INTEGER not null,
	is_hidden INTEGER not null,
	added_to_library INTEGER,
	last_played INTEGER,
	in_review INTEGER not null,
	note TEXT,
	song_cached_at INT,
	art_cached_at INT,
	offline_availability TEXT default 'NORMAL' not null,
	filesize_audio_ogg INT default 0 not null,
	filesize_art_png INT default 0 not null,
	filesize_thumbnail_png INT default 0 not null,
	thumbnail_cached_at INT,
	started_on_device INT,
	review_source_id INT,
	last_reviewed INT
);

create table sync_status (
	id INTEGER not null primary key autoincrement unique,
	sync_type TEXT not null,
	last_synced INTEGER not null,
	last_sync_attempted INTEGER not null
);

create table review_source (
	id INTEGER not null primary key unique,
	source_type TEXT not null,
	display_name TEXT not null,
	offline_availability TEXT not null,
	active INT default 1 not null
);

create table playlist_track (
	id INTEGER not null primary key unique,
	playlist_id INTEGER not null,
	track_id INTEGER not null,
	sort_order INTEGER not null,
	created_at INTEGER not null
);

create table playlist (
	id INT not null primary key unique,
	name TEXT not null,
	created_at INT not null,
	updated_at INT not null
);			
			""".trimIndent()
		)
	}
}
