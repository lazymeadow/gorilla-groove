ALTER TABLE `groovatron`.`track` 
CHANGE COLUMN `artist` `artist` VARCHAR(255) NOT NULL DEFAULT '' ,
CHANGE COLUMN `album` `album` VARCHAR(255) NOT NULL DEFAULT '' ,
CHANGE COLUMN `bit_rate` `bit_rate` INT(11) NOT NULL ,
ADD COLUMN `sample_rate` INT(11) NOT NULL AFTER `bit_rate`;
