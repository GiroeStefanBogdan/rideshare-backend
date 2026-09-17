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

## Reviews
- One directional review per member pair, for life. The pair is unique; a later shared ride reopens the
  existing review instead of creating a second one, and the reverse direction is a separate review.
- Reviews are eligible only from a qualifying shared ride: an active booking on an active ride whose
  passenger drop-off has passed, with author and recipient distinct. Cancelled experiences never
  qualify, and a driver's own self-booking never counts.
- The window is fourteen days from the passenger's scheduled drop-off. It is recomputed from the
  drop-off, never from submission time, and only a strictly later drop-off reopens it. Legacy rides
  with no schedule never open a window.
- Eligibility is derived from persisted bookings on every request. It is never accepted from a client,
  so a member cannot review a stranger by asserting a past ride.
- Content publishes when both counterparts hold a pending submission for the same cycle, or once its
  own window closes. A lone submission therefore becomes public at its deadline rather than waiting
  forever.
- Publication locks the cycle. Further edits require a later shared ride to reopen the window. Hidden
  reviews are also locked.
- An amendment never removes what is already public: the review keeps its last published score,
  comment, and role badge for public display and reputation while the pending values remain visible to
  their author only.
- Reputation is the arithmetic mean of published, non-hidden received reviews to one decimal, plus the
  count. Unrated is a null average with zero count, never a zero score. Hidden and pending content
  contributes nothing.
- The role badge records whether the review was written as a driver or as a passenger, on the most
  recent qualifying shared ride. Reputation itself is combined, not split by role.
- Moderators hide or restore published reviews. Hiding is reversible and never deletes; a hide or
  restore recomputes the affected member's reputation in the same transaction.
- Reviews outlive ride history. Account deletion anonymizes authored reviews to `Deleted member`,
  clears the author link, and removes the deleted member's own received reviews and reputation.
  Anonymization runs before the account row is removed, so a review never references a deleted account.
- A member cannot delete and recreate a review to escape the one-review rule, and there is no
  membership check: anyone may read published reviews and any member's rating summary.

## Vehicles
- A ride may reference one car owned by its driver. The association is optional and is cleared if that car is deleted.
- Public ride details expose brand, model, color, and year, but not license plate or registered seat count.
