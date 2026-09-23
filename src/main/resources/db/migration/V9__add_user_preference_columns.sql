-- Add smoking and pet-friendly preference columns to user_info.
-- Existing users without a UserInfo row receive an auto-created row with both preferences defaulting to false (i.e., "no preference").

-- Add columns (idempotent — safe to re-run on stale baselines)
ALTER TABLE user_info ADD COLUMN IF NOT EXISTS can_smoke boolean NOT NULL DEFAULT false;
ALTER TABLE user_info ADD COLUMN IF NOT EXISTS pet_friendly boolean NOT NULL DEFAULT false;

-- Create UserInfo rows for any users that don't already have one.
-- Uses a sub-select to only touch users missing a UserInfo row.
INSERT INTO user_info (user_id, bio, can_smoke, pet_friendly, rating, reviews_count)
SELECT u.id, NULL, false, false, NULL, 0
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM user_info ui WHERE ui.user_id = u.id
);
