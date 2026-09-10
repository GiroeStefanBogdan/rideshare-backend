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

## Retention
- A ride is finished after its final stop's `departsAt`.
- One month after finishing, a ride and its stops, bookings, and capacity records are eligible for permanent
  deletion as one aggregate.
- A location missing from refreshed source data is unavailable for new rides and location searches.
- A continuously missing location is eligible for permanent deletion six months after it was last confirmed in
  the source, provided no retained ride references it.
- A location confirmed by the current refresh remains available even when none of its attributes changed.
- When refreshed source records represent an existing settlement, its durable identity remains unchanged and the
  new boundary becomes current.
- A high-confidence source match preserves the durable location identity. A genuinely ambiguous match creates a
  new selectable location; unmatched prior candidates become unavailable rather than receiving guessed updates.

## Locations
- Selectable administrative units include cities, towns, villages, hamlets, suburbs, neighbourhoods, quarters,
  and localities. Counties provide hierarchy context but are not selectable.
- Selectable streets are named roads accessible to ordinary motor vehicles.
- Boundary-less settlements remain selectable, but streets are never assigned using inferred boundaries.
- A street crossing multiple administrative units belongs to the most specific containing unit in each segment.
- Location names follow current catalog data; rides do not preserve historical location snapshots.
- User-facing location names retain Romanian casing and diacritics. Original source names and normalized search
  text are distinct from display names.

## Vehicles
- A ride may reference one car owned by its driver. The association is optional and is cleared if that car is deleted.
- Public ride details expose brand, model, color, and year, but not license plate or registered seat count.
