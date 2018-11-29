CREATE TABLE `groovatron`.`user_library_history` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_library_id` INT UNSIGNED NOT NULL REFERENCES `user_library`(id) ON DELETE CASCADE,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`));

-- This was nullable before
ALTER TABLE `groovatron`.`user_library`
CHANGE COLUMN `created_at` `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ;
