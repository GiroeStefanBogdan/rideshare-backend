CREATE TABLE booking (
    id BIGSERIAL PRIMARY KEY,
    passenger_id BIGINT NOT NULL REFERENCES users(id),
    ride_id BIGINT NOT NULL REFERENCES ride(id),
    from_stop_id BIGINT NOT NULL REFERENCES ride_stop(id),
    to_stop_id BIGINT NOT NULL REFERENCES ride_stop(id),
    seats SMALLINT NOT NULL CHECK (seats > 0 AND seats <= 9),
    total_price INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
