CREATE TABLE `groovatron`.`crash_report`
(
    id int UNSIGNED PRIMARY KEY NOT NULL AUTO_INCREMENT,
    user_id int UNSIGNED NOT NULL REFERENCES `user`(id),
    size_kb int NOT NULL,
    version varchar(255) NOT NULL,
    device_type TINYINT UNSIGNED NOT NULL,
    created_at timestamp DEFAULT current_timestamp NOT NULL
);
