ALTER TABLE `groovatron`.`playlist`
ADD COLUMN `deleted` BIT(1) NOT NULL DEFAULT b'0' AFTER `created_at`;
