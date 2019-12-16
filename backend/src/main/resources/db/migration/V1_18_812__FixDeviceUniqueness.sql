-- Different users can use the same device, so make it unique across the device ID and user ID
ALTER TABLE `groovatron`.`device`
ADD UNIQUE INDEX `device_id_user_uniq` (`device_id` ASC, `user_id` ASC),
DROP INDEX `device_id_UNIQUE` ;
