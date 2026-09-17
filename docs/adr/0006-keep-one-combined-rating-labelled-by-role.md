# Keep one combined rating, labelled by role

Show a single combined reputation for a member: the arithmetic mean of published, non-hidden received
reviews to one decimal, plus the count. Each review carries a badge saying whether it was written as a
driver or as a passenger, but there are no separate driver and passenger averages.

## Consequences

- A member has one reputation to understand and one number to maintain, which matches the prototype's
  scale and the existing single `rating`/`reviews_count` columns on `user_info`.
- Search results, ride details, and booked cards keep working through the existing driver fields;
  no client has to choose between two scores.
- Role information is preserved at review level, so separate averages remain derivable later if the
  product needs them.
- Unrated is represented as no average with a zero count. It is never rendered as a zero score, since
  a new member is not a badly reviewed one.
- Reputation reads published content only, so a pending amendment cannot change a public average until
  it publishes.

See [review invariants](../../agents/domain.md#reviews).