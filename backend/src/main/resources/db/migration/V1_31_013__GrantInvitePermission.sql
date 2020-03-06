-- All current GG users should have invite powers. Grant it to them.
-- permission_type 2 is INVITE_USER
INSERT INTO groovatron.user_permission (user_id, permission_type)
SELECT id, 2 FROM groovatron.user WHERE deleted = 0;
