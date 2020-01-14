ALTER TABLE `groovatron`.`track_link`
ADD COLUMN `audio_format` TINYINT NOT NULL DEFAULT 0 AFTER `link`;
