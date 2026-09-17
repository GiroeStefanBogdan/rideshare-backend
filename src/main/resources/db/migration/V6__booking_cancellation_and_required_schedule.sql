-- Preserve legacy INACTIVE records while supporting explicit cancellation.
ALTER TABLE ride DROP CONSTRAINT IF EXISTS ride_status_check;
ALTER TABLE ride ADD CONSTRAINT ride_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'CANCELLED'));
ALTER TABLE booking DROP CONSTRAINT IF EXISTS booking_status_check;
ALTER TABLE booking ADD CONSTRAINT booking_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'CANCELLED'));

-- Do not fabricate schedules for legacy data. Reject new missing times even
-- where legacy nulls exist; validate and promote to NOT NULL when possible.
ALTER TABLE ride_stop ADD CONSTRAINT ride_stop_schedule_required CHECK (departs_at IS NOT NULL) NOT VALID;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM ride_stop WHERE departs_at IS NULL) THEN
        ALTER TABLE ride_stop VALIDATE CONSTRAINT ride_stop_schedule_required;
        ALTER TABLE ride_stop ALTER COLUMN departs_at SET NOT NULL;
    END IF;
END $$;
