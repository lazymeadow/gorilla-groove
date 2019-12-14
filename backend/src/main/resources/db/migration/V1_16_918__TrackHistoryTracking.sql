ALTER TABLE `groovatron`.`track_history`
ADD COLUMN `device_type` TINYINT NULL AFTER `track_id`,
ADD COLUMN `ip_address` VARCHAR(45) NULL AFTER `device_type`;
