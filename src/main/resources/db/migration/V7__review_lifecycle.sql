-- Review lifecycle: one directional lifetime review per user pair, with a reopening window.
-- Additive only; legacy rows become published so they keep their contribution to reputation.

ALTER TABLE user_reviews ALTER COLUMN target_user_id DROP NOT NULL;
ALTER TABLE user_reviews ALTER COLUMN reviewer_id DROP NOT NULL;
ALTER TABLE user_reviews ALTER COLUMN details DROP NOT NULL;
ALTER TABLE user_reviews ALTER COLUMN details TYPE varchar(1000);
ALTER TABLE user_reviews ALTER COLUMN date DROP NOT NULL;

ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS reviewer_name varchar(64);
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS status varchar(20);
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS reviewer_role varchar(20);
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS latest_shared_ride_id bigint;
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS latest_shared_dropoff_at timestamptz;
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS window_ends_at timestamptz;
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS submitted_at timestamptz;
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS published_at timestamptz;
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS published_score integer;
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS published_details varchar(1000);
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS published_role varchar(20);
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS hidden_at timestamptz;
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS hidden_reason varchar(200);
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS updated_at timestamptz;
ALTER TABLE user_reviews ADD COLUMN IF NOT EXISTS created_at timestamptz;

-- Legacy rows predate publication tracking: treat their existing content as already published.
UPDATE user_reviews
SET status = 'PUBLISHED',
    published_score = score,
    published_details = details,
    published_at = COALESCE(published_at, now()),
    updated_at = COALESCE(updated_at, now())
WHERE status IS NULL;

ALTER TABLE user_reviews ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE user_reviews ALTER COLUMN status SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uc_user_review_pair ON user_reviews (reviewer_id, target_user_id);