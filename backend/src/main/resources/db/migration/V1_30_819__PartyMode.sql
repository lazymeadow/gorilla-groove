ALTER TABLE `groovatron`.`device`
ADD COLUMN `party_enabled_until` TIMESTAMP NULL AFTER `additional_data`;

CREATE TABLE `groovatron`.`device_party_user` (
  `device_id` INT UNSIGNED NOT NULL REFERENCES `device`(id) ON DELETE CASCADE,
  `user_id` INT UNSIGNED NOT NULL REFERENCES `user`(id) ON DELETE CASCADE,
  UNIQUE INDEX `device_id_user_id` (`device_id` ASC, `user_id` ASC));
