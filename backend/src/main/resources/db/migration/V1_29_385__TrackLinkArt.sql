ALTER TABLE `groovatron`.`track_link`
ADD COLUMN `is_art` BIT(1) NOT NULL DEFAULT b'0' AFTER `audio_format`;
