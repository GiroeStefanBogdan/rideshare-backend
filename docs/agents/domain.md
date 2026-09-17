# Domain Invariants & Business Logic

## Users & Roles
- `ROLE_USER`: Standard account; can act as both **driver** and **passenger**.
- `ROLE_ADMIN`: Administrative operations only.

## Ride & Lifecycle
- **Status (persistence)**: `ACTIVE`, `CANCELLED` (explicit cancellation), or legacy `INACTIVE`
    (pre-feature status; accepted by constraints and mapped to `CANCELLED` in responses).
    - New rides are `ACTIVE`; there is no reactivation endpoint for cancelled/legacy inactive rides.
    - No persisted `COMPLETED` state; past vs. upcoming is dynamically derived from scheduled end
      times (see Time Windows below), not from status.
- **Creation limit**: `now < departureAt <= now.plusMonths(1)`.
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
- `GET /rides/me` keeps four arrays: upcoming/past bookings and upcoming/past hosted rides.
  Status does not determine which server time bucket contains an entry.
- **Scheduled end**: Booking drop-off `toStop.departsAt`; hosted final stop's `departsAt`
  (greatest `stopOrder`). Pickup/origin time is the start, not the history boundary.
- **Past Month**: Inclusive `[now.minusMonths(1), now]` by scheduled end, using one server
  clock snapshot and calendar-month subtraction, not a fixed 30-day duration. Older ends are omitted.
- **Upcoming**: End strictly after `now`, or legacy null end. Ongoing rides stay Upcoming.
- **Ordering**: Upcoming start ASC, Past end DESC; null keys last, stable booking/ride ID ASC ties.
- **Wire status**: A booking is `ACTIVE` only if booking and ride are both active; otherwise
  `CANCELLED`. Hosted status follows the ride, normalizing legacy `INACTIVE` to `CANCELLED`.
- **UI grouping**: Main Upcoming is active only; Cancelled merges both server buckets for each
  role. `/my-rides/past` shows only active Past. Cancellation is not completion.

## Cancellation
- Only the booking's passenger may cancel it. Missing/other-owner IDs yield 404 via the
  owner-filtered lookup; this is not a driver cancellation operation.
- An active booking on an active ride requires pickup strictly after `now`; pickup at/before
  `now` or null expires with HTTP 409 (see [API contract](../API.md#passenger-cancellation)).
- Already cancelled/legacy inactive booking or ride is a 204 no-op, even after pickup.
- Successful passenger cancellation marks only that booking `CANCELLED`, restoring exactly
  `booking.seats` on `[fromOrder, toOrder)`. Other bookings and the fare snapshot are preserved.
- One transaction looks up the owner's scalar ride ID, then locks **ride → booking → stops**
  with pessimistic write locks. Reservation also locks ride before stops; retries cannot restore twice.
- Driver `DELETE /rides/{id}` persists `CANCELLED` on the ride only. It does not delete stops,
  rewrite booking statuses, or restore segment seats; composed wire status shows affected bookings cancelled.
- New schedules require non-null stop times. Legacy null ends remain Upcoming without invented
  schedules; see [V6 migration](../DATABASE.md#migration-execution-and-v6).

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
