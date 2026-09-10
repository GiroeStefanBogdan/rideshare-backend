# Domain Invariants & Business Logic

## Users & Roles
- `ROLE_USER`: Standard account; can act as both **driver** and **passenger**.
- `ROLE_ADMIN`: Administrative operations only.

## Ride & Lifecycle
- **Status**: Only `ACTIVE` (available) or `INACTIVE` (cancelled/disabled).
    - No persisted `COMPLETED` state; past vs. upcoming is dynamically derived from `departureAt` relative to `now`.
- **Creation limit**: `departureAt` must be between `now` and `now + 1 month`.
- **Search visibility**: Only `ACTIVE` rides with available segment capacity are returned.

## Stops, Segments & Pricing
- **Route ordering**: Ordered contiguously by `RideStop.stopOrder`.
    - Booking invariant: `fromStop.stopOrder < toStop.stopOrder`.
- **Segment capacity**: Reserving `n` seats decrements capacity across each stop segment from `fromStop` up to (and including) `toStop - 1`.
- **Fare formula**: Stop prices are cumulative from the origin, begin at `0`, and strictly increase:
  $$\text{Fare} = (\text{toStop.cumulativePricePerSeat} -
  \text{fromStop.cumulativePricePerSeat}) \times \text{seats}$$

## Bookings
- Persists passenger, ride, `fromStop`, `toStop`, `seats`, `status`, and a snapshot of `totalPrice` at reservation time.
- **Testing quirk**: Drivers can currently reserve seats on their own hosted rides.

## Time Windows (Personal History)
- **Past Month**: Rolling interval `[now - 1 month, now)` based on `departureAt`.
- **Upcoming**: `departureAt >= now`.

## Vehicles
- A ride may reference one car owned by its driver. The association is optional and is cleared if that car is deleted.
- Public ride details expose brand, model, color, and year, but not license plate or registered seat count.
