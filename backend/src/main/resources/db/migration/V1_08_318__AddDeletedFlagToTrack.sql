ALTER TABLE `groovatron`.`track`
ADD COLUMN `deleted` BIT NOT NULL DEFAULT 0 AFTER `last_played`;
