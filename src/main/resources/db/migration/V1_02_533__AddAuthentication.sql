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

-- The first user has to be made directly in the DB, as there is no user creation for anonymous users
-- This is mostly for the aid of future tests, and development. But the user should be deleted when deployed
-- Password unencrypted is "dude"
INSERT INTO user(id, name, email, password)
  VALUES ('2', 'dude', 'dude@dude.dude', '$2a$10$tYEIuYjlND8TzgwD5sdShuktlQIALkNHtET3yiT6iyROyPgMhtSJK')

