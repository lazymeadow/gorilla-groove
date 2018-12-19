ALTER TABLE `groovatron`.`user_library`
RENAME TO  `groovatron`.`track` ;

ALTER TABLE `groovatron`.`user_library_history`
RENAME TO  `groovatron`.`track_history` ;

ALTER TABLE `groovatron`.`track_history`
CHANGE COLUMN `user_library_id` `track_id` INT(10) UNSIGNED NOT NULL ;

ALTER TABLE `groovatron`.`playlist_track`
CHANGE COLUMN `user_library_id` `track_id` INT(10) UNSIGNED NOT NULL ;


