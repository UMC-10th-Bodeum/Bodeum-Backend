-- Rename guardian type enum value stored in guardian_profiles.
-- Apply before deploying code that changes GuardianType.OTHER to GuardianType.ETC.

UPDATE guardian_profiles
SET guardian_type = 'ETC'
WHERE guardian_type = 'OTHER';
