--ALTER TABLE `user_library`
--DROP FOREIGN KEY `FKc0yldqsbglvvenxcqjuh39tvc`;
ALTER TABLE `user_library`
DROP COLUMN `track_id`,
CHANGE COLUMN `created_at` `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `hidden`,
ADD COLUMN `name` VARCHAR(255) NOT NULL AFTER `user_id`,
ADD COLUMN `artist` VARCHAR(255) NOT NULL DEFAULT '' AFTER `name`,
ADD COLUMN `album` VARCHAR(255) NOT NULL DEFAULT '' AFTER `artist`,
ADD COLUMN `file_name` VARCHAR(255) NOT NULL AFTER `album`,
ADD COLUMN `bit_rate` INT(11) NOT NULL DEFAULT 0 AFTER `file_name`,
ADD COLUMN `sample_rate` INT(11) NOT NULL AFTER `bit_rate`,
ADD COLUMN `length` INT(11) NOT NULL AFTER `sample_rate`,
ADD COLUMN `release_year` INT(11) NULL AFTER `length`,
ADD INDEX `ruewiofjewaiforewa_idx` (`user_id` ASC),
DROP INDEX `unique_track_user` ;
ALTER TABLE `user_library`
ADD CONSTRAINT `ruewiofjewaiforewa`
  FOREIGN KEY (`user_id`)
  REFERENCES `user` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

--ALTER TABLE `playlist_track`
--DROP FOREIGN KEY `FKf9qyasbwmrlc4tb1vfll54bot`;
ALTER TABLE `playlist_track`
CHANGE COLUMN `track_id` `user_library_id` INT(10) UNSIGNED NOT NULL ;
ALTER TABLE `playlist_track`
ADD CONSTRAINT `FKf9qyasbwmrlc4tb1vfll54bot`
  FOREIGN KEY (`user_library_id`)
  REFERENCES `track` (`id`);

ALTER TABLE `playlist_track`
DROP FOREIGN KEY `FKf9qyasbwmrlc4tb1vfll54bot`;
ALTER TABLE `playlist_track`
DROP INDEX `FKf9qyasbwmrlc4tb1vfll54bot` ,
ADD INDEX `FKf9qyasbwmrlc4tb1vfll54bot_idx` (`user_library_id` ASC);
ALTER TABLE `playlist_track`
ADD CONSTRAINT `FKf9qyasbwmrlc4tb1vfll54bot`
  FOREIGN KEY (`user_library_id`)
  REFERENCES `user_library` (`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

DROP TABLE `track`;