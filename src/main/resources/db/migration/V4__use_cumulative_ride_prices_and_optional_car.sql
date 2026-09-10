DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'ride' AND column_name = 'total_price_per_seat'
    ) THEN
        ALTER TABLE ride RENAME COLUMN price_per_seat TO total_price_per_seat;
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'ride' AND column_name = 'price_per_seat'
    ) THEN
        UPDATE ride SET total_price_per_seat = price_per_seat;
        ALTER TABLE ride DROP COLUMN price_per_seat;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'ride_stop' AND column_name = 'cumulative_price_per_seat'
    ) THEN
        ALTER TABLE ride_stop RENAME COLUMN price_per_seat TO cumulative_price_per_seat;
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'ride_stop' AND column_name = 'price_per_seat'
    ) THEN
        UPDATE ride_stop SET cumulative_price_per_seat = price_per_seat;
        ALTER TABLE ride_stop DROP COLUMN price_per_seat;
    END IF;
END $$;

UPDATE ride_stop AS stop
SET cumulative_price_per_seat = ride.total_price_per_seat - stop.cumulative_price_per_seat
FROM ride
WHERE stop.ride_id = ride.id;

UPDATE ride
SET total_price_per_seat = destination.cumulative_price_per_seat
FROM ride_stop AS destination
WHERE destination.ride_id = ride.id
  AND destination.stop_order = (
      SELECT MAX(last_stop.stop_order)
      FROM ride_stop AS last_stop
      WHERE last_stop.ride_id = ride.id
  );

ALTER TABLE ride_stop ALTER COLUMN cumulative_price_per_seat SET NOT NULL;
ALTER TABLE ride ALTER COLUMN total_price_per_seat SET NOT NULL;
ALTER TABLE ride ADD COLUMN IF NOT EXISTS car_id BIGINT;

DO $$
DECLARE
    existing_constraint TEXT;
BEGIN
    FOR existing_constraint IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints AS tc
        JOIN information_schema.key_column_usage AS kcu
          ON tc.constraint_name = kcu.constraint_name
         AND tc.constraint_schema = kcu.constraint_schema
        WHERE tc.table_name = 'ride'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'car_id'
    LOOP
        EXECUTE format('ALTER TABLE ride DROP CONSTRAINT %I', existing_constraint);
    END LOOP;

    ALTER TABLE ride
        ADD CONSTRAINT fk_ride_car FOREIGN KEY (car_id) REFERENCES user_cars(id) ON DELETE SET NULL;
END $$;
