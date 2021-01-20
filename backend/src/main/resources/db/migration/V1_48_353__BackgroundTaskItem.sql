CREATE TABLE background_task_item
(
	id int not null primary key AUTO_INCREMENT,
    user_id INT NOT NULL REFERENCES `user`(id),
    device_id INT NOT NULL REFERENCES `device`(id),
	status TINYINT NOT NULL,
	type TINYINT NOT NULL,
	payload TEXT NOT NULL,
	description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
