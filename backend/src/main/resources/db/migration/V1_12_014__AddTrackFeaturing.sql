ALTER TABLE `groovatron`.`track`
ADD COLUMN `featuring` VARCHAR(128) NOT NULL AFTER `artist`;
