CREATE TABLE `groovatron`.`device` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL,
  `device_id` VARCHAR(64) NOT NULL,
  `device_type` TINYINT UNSIGNED NOT NULL,
  `application_version` VARCHAR(64) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC),
  UNIQUE INDEX `device_id_UNIQUE` (`device_id` ASC),
  INDEX `device_version_user_idx` (`user_id` ASC),
  CONSTRAINT `device_version_user`
    FOREIGN KEY (`user_id`)
    REFERENCES `groovatron`.`user` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

ALTER TABLE `groovatron`.`track_history`
DROP COLUMN `device_type`,
ADD COLUMN `device_id` INT UNSIGNED NULL AFTER `track_id`;

ALTER TABLE `groovatron`.`track_history`
ADD INDEX `fewafewfewarewajfieow123_idx` (`device_id` ASC);
ALTER TABLE `groovatron`.`track_history`
ADD CONSTRAINT `FKtrackhistorydeviceid`
  FOREIGN KEY (`device_id`)
  REFERENCES `groovatron`.`device` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

ALTER TABLE `groovatron`.`device`
ADD COLUMN `last_ip` VARCHAR(45) NOT NULL AFTER `application_version`;

