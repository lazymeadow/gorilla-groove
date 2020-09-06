ALTER TABLE user_token ADD device_id int UNSIGNED NULL AFTER user_id;
ALTER TABLE user_token
ADD CONSTRAINT user_token_device_id_fk
FOREIGN KEY (device_id) REFERENCES device (id) ON DELETE SET NULL;
