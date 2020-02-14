CREATE TABLE `groovatron`.`user_invite_link` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `link_identifier` CHAR(36) NOT NULL,
  `inviting_user_id` INT UNSIGNED NOT NULL REFERENCES `user`(id),
  `created_at` TIMESTAMP NOT NULL DEFAULT now(),
  `expires_at` TIMESTAMP NOT NULL,
  `created_user_id` INT UNSIGNED NULL REFERENCES `user`(id),
  PRIMARY KEY (`id`),
  UNIQUE INDEX `link_identifier_UNIQUE` (`link_identifier` ASC),
  INDEX `a_idx` (`inviting_user_id` ASC, `created_user_id` ASC))
;
