# Use explicit CANCELLED status

Persist `CANCELLED` for new passenger and driver cancellations rather than overloading `INACTIVE`.
Cancellation is a lifecycle decision, independent of time-derived Upcoming/Past, and does not delete
history. Passenger cancellation changes only the booking; driver cancellation changes only the ride.

## Compatibility and consequences

- Keep legacy `INACTIVE` accepted in persistence. V6 expands both status checks without rewriting
  old records; the backend normalizes legacy values to `CANCELLED` in My Rides responses.
- My Rides wire status is only `ACTIVE` / `CANCELLED`. A booking is active only when both booking
  and ride are active, so driver cancellation requires no bulk booking-status updates.
- Retain the four response arrays and classify by scheduled end, independently of cancellation.
  Clients merge cancelled entries from both time buckets; Past UI shows active entries only.
- Passenger cancellation restores exactly its segment seats once in a ride-first locked transaction.
  A cancelled/inactive booking or ride is a no-op, preserving retry safety.

See [API](../API.md#passenger-cancellation) and [V6](../DATABASE.md#migration-execution-and-v6).
