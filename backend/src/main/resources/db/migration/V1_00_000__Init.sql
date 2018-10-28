CREATE TABLE `groovatron`.`user` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`id`));

CREATE TABLE `groovatron`.`track` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `artist` VARCHAR(255) NULL,
  `album` VARCHAR(255) NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `play_count` INT NOT NULL DEFAULT 0,
  `bit_rate` INT NULL,
  `length` INT NOT NULL,
  `release_year` INT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_played` TIMESTAMP NULL,
  PRIMARY KEY (`id`));

