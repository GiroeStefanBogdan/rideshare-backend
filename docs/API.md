# REST API Contract

Base URL: `http://localhost:8080`. Auth: HTTP-only `token` cookie (`credentials: 'include'`).

## Error Format
Failures return `ErrorResponseDto`:
```json
{
  "timestamp": "2026-07-14T12:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "field: reason"
}
```
Standard errors return 400, 404, or 409. Scheduling/pricing domain failures return **422 Unprocessable Entity**.

---

## Endpoints

### Dashboard & Locations
| Method | Path | Auth | Request | Response | Notes |
|---|---|---|---|---|---|
| `GET` | `/dashboard` | Public | — | 200 String | Username if authenticated; empty/null otherwise |
| `GET` | `/locations/search?q={query}` | Public | — | 200 `LocationResultDTO[]` | Returns `[]` if query < 2 trimmed chars |

### Auth & Users
| Method | Path | Auth | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/register` | Public | `UserRegistrationRequestDto` | 201 `UserResponseDto` |
| `POST` | `/login` | Public | `LoginRequest` | 200 `LoginResponse` (sets `token` cookie) |
| `POST` | `/auth/logout` | Public | — | 204 (clears `token` cookie) |
| `GET` | `/users` | Admin | — | 200 `UserResponseDto[]` |
| `GET` | `/users/{id}` | Public | — | 200 `UserPublicProfileDto` |
| `GET` | `/users/me` | User | — | 200 `UserProfileDto` |
| `PATCH` | `/users/me` | User | `UpdateUserRequest` | 200 `UserResponseDto` |
| `PATCH` | `/users/me/password` | User | `LoginRequest` (uses password) | 204 |
| `DELETE`| `/users/me` | User | — | 204 (clears `token` cookie) |
| `PATCH` | `/admin/users/{id}/role` | Admin | JSON string `Role` | 200 `UserResponseDto` |
| `DELETE`| `/admin/users/{id}` | Admin | — | 204 |

### User Cars
| Method | Path | Auth | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/users/me/cars` | User | `UserCarRequest` | 201 `UserCarResponse` |
| `GET` | `/users/me/cars` | User | — | 200 `UserCarResponse[]` |
| `PATCH` | `/users/me/cars/{carId}` | User | `UpdateUserCarRequest` | 200 `UserCarResponse` |
| `DELETE`| `/users/me/cars/{carId}` | User | — | 204 |

### Rides & Bookings
| Method | Path | Auth | Request Body | Response | Notes |
|---|---|---|---|---|---|
| `POST` | `/rides` | User | `RideDTO` | 201 `{ "rideId": number }` | |
| `GET` | `/rides/{id}` | Public | — | 200 `RideDetailsDTO` | |
| `PATCH` | `/rides/{id}/seats` | Owner | Raw JSON integer/byte | 204 | Body is a raw number, not an object |
| `DELETE`| `/rides/{id}` | Owner | — | 204 | Persists `CANCELLED` on the ride only; preserves bookings, stops, and capacity |
| `POST` | `/rides/search` | Public | `RideSearchRequestDTO` | 200 `RideSearchResultDTO[]` | |
| `POST` | `/rides/{id}/reserve` | User | `ReserveRideRequestDTO` | 201 `{ "bookingId": number }` | |
| `GET` | `/rides/me` | User | — | 200 `MyRidesResponseDTO` | Four end-time buckets; see below |
| `DELETE` | `/rides/me/bookings/{bookingId}` | Booking owner | — | 204, no body | Passenger cancellation; missing/other passenger's booking returns 404 |

### My Rides classification

`GET /rides/me` retains `upcomingBookings`, `pastBookings`, `upcomingHostedRides`, and
`pastHostedRides`; there is no separate past API endpoint or role query parameter.

- Scheduled end is the booking's drop-off `toStop.departsAt`, or the hosted ride's final
  stop `departsAt` (highest `stopOrder`), not its departure time.
- Using one server `now`, Past is `now.minusMonths(1) <= end <= now`, both boundaries inclusive.
  The cutoff is one calendar month, not 30 days. Earlier ends are omitted.
- Upcoming means `end > now` or a legacy null end. An ongoing ride remains Upcoming until its end.
- Upcoming sorts by start ascending (booking pickup / hosted `departureAt`); Past sorts by end
  descending. Null sort keys are last; ties use booking ID or ride ID ascending.
- Both time buckets may contain cancellations. Wire status is `ACTIVE` only when both booking
  and ride are active; otherwise a booking is `CANCELLED`. Hosted status follows the ride.
  Persisted legacy `INACTIVE` maps to wire `CANCELLED`.

The UI shows active Upcoming plus Cancelled merged from both time buckets on `/my-rides`, and
only active Past on `/my-rides/past`; these are frontend routes, not additional API resources.

### Passenger cancellation

`DELETE /rides/me/bookings/{bookingId}` requires the authenticated passenger who owns the booking.
A nonexistent ID or another passenger's ID returns **404** (`Booking Not Found`).

- An active booking on an active ride can be cancelled strictly before its own pickup:
  `fromStop.departsAt > now`. At/after pickup, or with a legacy null pickup, cancellation expires.
- Expired cancellation returns **409 Conflict** (`Booking Cancellation Expired`).
  Error `message` may be null.
- Already `CANCELLED`/legacy `INACTIVE` booking **or ride** returns **204** as a no-op,
  before checking pickup time; no status or capacity is changed.
- Successful cancellation persists `CANCELLED` on the booking and restores exactly its `seats`
  on stop segments `[fromOrder, toOrder)`, excluding the drop-off segment. Other bookings,
  the ride status, and the total-price snapshot are unchanged.
- One transaction resolves the owner's scalar ride ID, locks ride, then booking, then stops
  (pessimistic write). The common ride-first lock order serializes reservation/cancellation and
  prevents repeated cancellation from releasing capacity twice.


---

## DTO Models & Fields

- **UserRegistrationRequestDto**: `name`, `email`, `password`, `birthday`, `phoneNumber`, `gender`
- **LoginRequest**: `email`, `password`, `rememberMe`
- **UserResponseDto**: `id`, `name`, `email`, `role`, `phoneNumber`, `birthday`, `gender`
- **UserPublicProfileDto**: `id`, `name`, `birthday`, `gender`
- **UpdateUserRequest** (nullable partials): `name`, `email`, `phoneNumber`, `birthday`, `gender`
- **UserCarRequest** / **UpdateUserCarRequest**: `brand`, `model`, `color`, `year`, `licensePlate`, `numberOfSeats` (`UserCarResponse` adds `id`, `userId`)
- **LocationResultDTO**: `id`, `type`, `name`, `fullName`, `latitude`, `longitude`, `population`
- **RideDTO**: `seatsTotal` (1–4), optional `carId`, and 2–7
  `rideStops: [{ id, type, stopOrder, cumulativePricePerSeat, departsAt }]`. Stop entries and
  every `departsAt` are required; locations are distinct, times are at least one minute apart,
  the origin price is `0`, and later cumulative prices strictly increase.
- **RideSearchRequestDTO**:
    - *Required*: `fromId`, `fromType`, `toId`, `toType`, `date`, `seats`
    - *Optional*: `maxDistanceStart`, `maxDistanceEnd`, `maxPrice`, `timeWindow`, `smokingAllowed`, `petFriendly`
- **RideSearchResultDTO**: `rideId`, `driver` (`RideDriverDTO`), `seatsAvailable`, `totalPrice`, `startStop` (`RideStopBasicDTO`), `endStop` (`RideStopBasicDTO`), `distanceToStartKm`, `distanceToEndKm`
- **RideDetailsDTO**: `rideId`, `driver`, `seatsTotal`, optional public `vehicle`,
  `rideStops: [{ locationName, municipalityName, departsAt, availableSeats, cumulativePricePerSeat }]`
- **ReserveRideRequestDTO**: `fromStopId`, `toStopId`, `seats`
- **MyRidesResponseDTO**:
    - `upcomingBookings` / `pastBookings`: `[{ bookingId, rideId, status, driver, seats, totalPrice, fromStop, toStop }]`
    - `upcomingHostedRides` / `pastHostedRides`: `[{ rideId, status, seatsTotal, rideStops }]`

---

## Enums
- **Role**: `ROLE_USER`, `ROLE_ADMIN`
- **Gender**: `MALE`, `FEMALE`
- **AuthProvider**: `LOCAL`, `GOOGLE`
- **Status** (persistence): `ACTIVE`, `CANCELLED`, legacy `INACTIVE`. My Rides emits only
  `ACTIVE` / `CANCELLED`; legacy `INACTIVE` is normalized by the backend.
- **TimeWindow**: `BEFORE_8`, `8_12`, `12_18`, `AFTER_18`

## Contract Gotchas & Gaps
- `POST /login` returns `{ token, user }` in the response body despite the client being cookie-only.
- Review endpoints referenced in frontend code do not exist in the backend.
