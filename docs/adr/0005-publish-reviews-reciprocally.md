# Publish reviews reciprocally, and lock on publication

A review's content stays Pending until it becomes public. It publishes when both counterparts have
submitted for the same cycle, or unilaterally when its own window closes. Publication ends that cycle,
so editing is locked even if calendar time remains. A later shared ride reopens the cycle for further
input.

## Consequences

- Neither side can read the other's review before writing their own, which limits retaliation while
  both are still writing.
- A member who never submits still receives their counterpart's review once that counterpart's window
  closes, so a lone review is not withheld forever.
- Editing after publication would leak the counterpart's score and invite a response, so an update
  must wait for a later shared ride. This knowingly trades flexibility for that protection.
- An amendment made before re-publication must not disturb what is already public: the review keeps
  its last published score, comment, and role for public display and reputation, while the pending
  values are returned only to their author.
- Publication is evaluated lazily whenever a member's reviews are read or written, so deadlines are
  honoured without a scheduler. Reputation is recomputed in the same transaction as publication,
  hiding, restoring, or account anonymization.
- Hidden reviews leave reputation and public listings but are retained for moderation.

See [review invariants](../../agents/domain.md#reviews).