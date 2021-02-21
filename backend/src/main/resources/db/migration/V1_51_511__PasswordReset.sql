CREATE TABLE password_reset
(
    id int UNSIGNED PRIMARY KEY NOT NULL AUTO_INCREMENT,
    user_id int UNSIGNED NOT NULL REFERENCES `user`(id) ON DELETE CASCADE,
    unique_key char(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX password_reset_unique_key_uindex ON password_reset (unique_key);
