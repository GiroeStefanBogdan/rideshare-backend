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
- **Fare formula**: Stop prices are cumulative and decrease to `0` at destination:
  $$\text{Fare} = (\text{fromStop.pricePerSeat} - \text{toStop.pricePerSeat}) \times \text{seats}$$

## Bookings
- Persists passenger, ride, `fromStop`, `toStop`, `seats`, `status`, and a snapshot of `totalPrice` at reservation time.
- **Testing quirk**: Drivers can currently reserve seats on their own hosted rides.

## Time Windows (Personal History)
- **Past Month**: Rolling interval `[now - 1 month, now)` based on `departureAt`.
- **Upcoming**: `departureAt >= now`.

## Known Gaps
- Soft-deleting a ride (`INACTIVE`) currently also hard-deletes associated `RideStop` records, breaking historical itinerary references for past bookings.