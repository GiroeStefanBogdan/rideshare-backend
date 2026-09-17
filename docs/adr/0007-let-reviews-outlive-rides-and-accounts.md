# Let reviews outlive rides and accounts

A review persists after its ride leaves personal history, and after either participant deletes their
account. Deleting an author keeps their review's contribution to the counterpart's reputation, clears
the author link, and attributes it to `Deleted member`. Deleting a recipient removes that member's
received reviews and therefore their reputation.

## Consequences

- Reputation stays stable across history expiry and any future ride purge, so rating a member never
  depends on how long ago the ride happened.
- A pair's uniqueness survives account deletion: clearing the reviewer link also removes the row from
  the unique pair index, so a re-registered member is not shadowed by an old account.
- Comment text written by a deleted author remains addressed to the surviving counterpart, who is the
  party it describes; it is not personal data of the deleted member's own profile. Requests to remove
  personal information from that text remain a moderation action.
- Anonymization runs inside account deletion, before the row is removed, so no review ever references
  a deleted account and deletion cannot fail on a review foreign key.
- Deleting an account still requires unrelated owned records to be resolved; reviews are handled here,
  while other references are outside this decision.

See [review invariants](../../agents/domain.md#reviews) and
[cancellation ADR](0003-use-explicit-cancelled-status.md) for the same "keep history" stance.