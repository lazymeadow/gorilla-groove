ALTER TABLE `groovatron`.`track_history`
ADD COLUMN `deleted` BIT NOT NULL DEFAULT b'0' AFTER `ip_address`;
