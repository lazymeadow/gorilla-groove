ALTER TABLE track_history ADD utc_listened_at timestamp DEFAULT current_timestamp NOT NULL AFTER listened_in_review;

-- Up until now, no clients have really been able to send play history in the past, so this is good enough.
-- Would technically be slightly more accurate to convert timezone based off the local listened time,
-- but that sounds annoying and not worth
UPDATE track_history SET utc_listened_at = created_at;
