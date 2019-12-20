CREATE TABLE `groovatron`.`user_permission` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL,
  `permission_type` TINYINT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `fkuserpermissionuser_idx` (`user_id` ASC),
  CONSTRAINT `fkuserpermissionuser`
    FOREIGN KEY (`user_id`)
    REFERENCES `groovatron`.`user` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);
