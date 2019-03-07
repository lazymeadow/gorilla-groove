CREATE TABLE `groovatron`.`track_link` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `track_id` INT UNSIGNED NOT NULL REFERENCES track(id),
  `link` VARCHAR(512) NOT NULL,
  `expires_at` TIMESTAMP NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC));
