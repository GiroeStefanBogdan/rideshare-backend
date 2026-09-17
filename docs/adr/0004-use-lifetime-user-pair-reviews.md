# Use lifetime user-pair reviews with reopening windows

Keep at most one directional review per member pair instead of one review per ride or per booking.
The pair `(reviewer, target)` stays unique for life. A later shared ride does not create a second
review; it reopens the existing one for fourteen days, measured from the newer scheduled drop-off.

## Consequences

- Reputation cannot be inflated by repeated rides, extra seats, or split bookings, and the existing
  unique constraint keeps its meaning.
- Someone who never wrote a first review still gets a fresh window from any later qualifying ride.
- Role labels describe the most recent qualifying shared ride, so members who swap roles over time
  are labelled by what they last did.
- The window deadline is always recomputed from the drop-off, never from submission time, so delaying
  a submission cannot extend eligibility.
- Eligibility remains derived from persisted bookings (drop-off passed, booking and ride both active,
  author and recipient distinct) rather than from anything a client asserts. Cancelled experiences
  never qualify.
- Schedule-derived eligibility is not proof of attendance. A member who booked and never travelled can
  still review; reporting a no-show is a separate capability.

See [review invariants](../../agents/domain.md#reviews) and [V7](../../DATABASE.md#review-lifecycle-and-v7).