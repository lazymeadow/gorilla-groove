-- These exist just for record keeping if someone screws up merges or deletions and needs data manually put back
ALTER TABLE track_history ADD original_device_id int unsigned NULL;

ALTER TABLE device ADD original_merged_device_id int unsigned NULL;
