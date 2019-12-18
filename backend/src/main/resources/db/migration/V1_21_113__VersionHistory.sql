CREATE TABLE `groovatron`.`version_history` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `version` VARCHAR(64) NOT NULL,
  `device_type` INT UNSIGNED NOT NULL,
  `notes` TEXT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`));
