ALTER TABLE `groovatron`.`track`
  CHANGE COLUMN `id` `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT ;

CREATE TABLE `groovatron`.`user_library` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL REFERENCES `user`(id) ON DELETE CASCADE,
  `track_id` INT UNSIGNED NOT NULL REFERENCES `track`(id) ON DELETE CASCADE,
  `play_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `hidden` BIT NOT NULL DEFAULT 0,
  `last_played` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`));

ALTER TABLE `groovatron`.`track`
  DROP COLUMN `last_played`;

ALTER TABLE `groovatron`.`user_library`
  ADD UNIQUE INDEX `unique_track_user` (`user_id` ASC, `track_id` ASC);

