ALTER TABLE `groovatron`.`device`
ADD COLUMN `device_name` VARCHAR(128) NULL AFTER `device_id`;

UPDATE `groovatron`.`device` SET device_name = device_id WHERE id > 0;

ALTER TABLE `groovatron`.`device`
CHANGE COLUMN `device_name` `device_name` VARCHAR(128) NOT NULL ;

ALTER TABLE `groovatron`.`device`
ADD UNIQUE INDEX `device_name_user_uniq` (`user_id` ASC, `device_name` ASC);

-- Deleting device sets device_id as NULL for track_history
ALTER TABLE `groovatron`.`track_history`
DROP FOREIGN KEY `FKtrackhistorydeviceid`;
ALTER TABLE `groovatron`.`track_history`
ADD CONSTRAINT `FKtrackhistorydeviceid`
  FOREIGN KEY (`device_id`)
  REFERENCES `groovatron`.`device` (`id`)
  ON DELETE SET NULL;

-- Add parent device ID for merging, and the FK for it
ALTER TABLE `groovatron`.`device`
ADD COLUMN `merged_device_id` INT UNSIGNED NULL AFTER `device_id`,
ADD COLUMN `archived` BIT(1) NOT NULL DEFAULT b'0' AFTER `device_type`,
ADD INDEX `merged_device_id_id_idx` (`merged_device_id` ASC);
ALTER TABLE `groovatron`.`device`
ADD CONSTRAINT `merged_device_id_id`
  FOREIGN KEY (`merged_device_id`)
  REFERENCES `groovatron`.`device` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

