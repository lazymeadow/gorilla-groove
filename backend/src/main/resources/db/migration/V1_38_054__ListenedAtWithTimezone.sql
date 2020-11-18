ALTER TABLE track_history ADD local_time_listened_at datetime NULL;
UPDATE track_history SET local_time_listened_at = created_at;
ALTER TABLE track_history MODIFY local_time_listened_at datetime NOT NULL;

-- I genuinely don't know if I'll have a use for this,
-- but it seems like a fun, and potentially useful, bit of data to store. So I will
ALTER TABLE track_history ADD iana_timezone varchar(255) DEFAULT 'America/Boise' NOT NULL;

-- Commented these out as they only need to be run in prod, and I need to potentially do a DB update before they will work on the EC2.
-- So I'm just leaving them in here for record keeping purposes
-- UPDATE track_history SET local_time_listened_at = CONCAT(created_at, '+07:00');
-- UPDATE track_history SET local_time_listened_at = CONCAT(created_at, '+06:00') WHERE created_at > '2019-03-10' AND created_at < '2019-11-03';
-- UPDATE track_history SET local_time_listened_at = CONCAT(created_at, '+06:00') WHERE created_at > '2020-03-08' AND created_at < '2020-11-01';

