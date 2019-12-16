-- Rename 'hidden' to be 'private' as that better matches what it's doing
ALTER TABLE `groovatron`.`track`
CHANGE COLUMN `hidden` `private` BIT(1) NOT NULL DEFAULT b'0' ;

-- Add a new column for 'hidden' that just means hidden from yourself
ALTER TABLE `groovatron`.`track`
ADD COLUMN `hidden` BIT(1) NOT NULL DEFAULT b'0' AFTER `private`;

