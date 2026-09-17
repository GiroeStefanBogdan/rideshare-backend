# RideShare Context

DrumBun connects rides and their stops to durable, searchable geographic locations.

## Locations

**Location**:
A user-selectable geographic ride endpoint with a durable DrumBun identity. Its map-derived details may change
without changing that identity.
_Avoid_: OSM record, search result

**Administrative unit**:
A settlement or named subdivision that can serve as a ride endpoint, such as a city, town, village, hamlet,
suburb, neighbourhood, quarter, or locality. Counties provide context but are not selectable.

**Street**:
A named, motor-accessible road associated with one administrative unit. A road crossing administrative units is
represented as a separate selectable street in each one. A street is a location, not an administrative unit.

**OSM source record**:
One replaceable OpenStreetMap representation from which a location's geographic details originate. Multiple
source records may describe the same durable location.
_Avoid_: Location

**Source name**:
The Romanian name supplied by `name:ro`, falling back to `name`, without editorial rewriting.

**Display name**:
The user-facing, conservatively beautified name of a location.
_Avoid_: Normalized name

**Selectable location**:
A user-selectable administrative unit or street. Addresses, points of interest, counties, and arbitrary GPS pins
are not selectable locations.

**Unavailable location**:
A durable location absent from the latest source refresh and therefore unavailable in searches and new rides.
Existing rides may continue to reference its current details.
_Avoid_: Deleted location


## My Rides and cancellation

**Booking**:
A passenger's persisted seat allocation between pickup and drop-off stops on a ride, including
its seat count and fare snapshot.

**Booked / Hosted**:
The passenger's bookings / the driver's published rides. These are My Rides roles, not account roles.

**Scheduled end**:
The booking's drop-off time or the hosted ride's final-stop time, used for personal-history classification.
It is not the pickup/origin departure time.

**Upcoming / Past**:
Server time buckets independent of status. Upcoming has end after now or a legacy null end; Past
has end in the inclusive calendar-month interval `[now.minusMonths(1), now]`. An ongoing ride
remains Upcoming; older ends are omitted from My Rides.

**Cancelled**:
A booking or ride no longer active by lifecycle decision, not because its schedule ended. New
cancellations persist `CANCELLED`; legacy `INACTIVE` maps to the same wire status. A booking appears
cancelled if either it or its ride is cancelled/inactive. The UI merges cancellations from both
server time buckets rather than showing them in active Past.

**Passenger cancellation**:
The booking owner's action strictly before pickup, restoring that booking's seats once on
`[fromOrder, toOrder)` and changing only its status. Already cancelled/inactive booking or ride
is a no-op. Driver cancellation instead changes only the ride status and releases no seats.

See [domain invariants](docs/agents/domain.md) and [cancellation ADR](docs/adr/0003-use-explicit-cancelled-status.md).


## Reviews and reputation

**Review**:
One directional piece of feedback between two members who shared a ride: a passenger reviewing their
driver, or a driver reviewing one of their booking passengers. A member keeps at most one review per
counterpart for life; a later shared ride reopens its window instead of creating another review.
_Avoid_: Ride review, per-booking review, second review

**Review window**:
The fourteen days following the passenger's scheduled drop-off during which a review may be submitted
or amended. It reopens for a strictly later shared drop-off.
_Avoid_: Deadline, grace period

**Pending / Published**:
A review's current content is Pending until it becomes public. A review publishes once both
counterparts have submitted for the same cycle, or once its own window closes. Published reviews are
visible to everyone and count toward reputation; pending ones are visible only to their author.
_Avoid_: Draft, unpublished, visible

**Hidden**:
A published review removed from public sight by moderation. It stops counting toward reputation but is
not deleted.
_Avoid_: Deleted review, rejected review

**Rating summary**:
A member's combined reputation across published, non-hidden received reviews: their arithmetic mean to
one decimal and the number of reviews. A member without published reviews is unrated, which is not the
same as a zero score.
_Avoid_: Stars, driver rating, passenger rating

**Deleted member**:
The attribution given to a review whose author has deleted their account. Its contribution to the
counterpart's reputation remains; the deleted member's own listing and reputation do not.
_Avoid_: Anonymous user, removed review

See [review ADRs](docs/adr/0004-use-lifetime-user-pair-reviews.md) and
[review invariants](docs/agents/domain.md#reviews).
