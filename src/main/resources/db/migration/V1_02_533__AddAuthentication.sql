ALTER TABLE `groovatron`.`user`
ADD COLUMN `email` VARCHAR(255) NOT NULL AFTER `name`,
ADD COLUMN `password` VARCHAR(255) AFTER `email`;

-- Forgot to make the PK unsigned, which breaks the FK constraint on the new user_token table
ALTER TABLE `groovatron`.`user`
CHANGE COLUMN `id` `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT ;

CREATE TABLE `groovatron`.`user_token` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL REFERENCES `user`(id) ON DELETE CASCADE,
  `token` CHAR(36) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`));

ALTER TABLE `groovatron`.`user`
ADD UNIQUE INDEX `unique_email` (`email` ASC);
