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
| `DELETE`| `/rides/{id}` | Owner | — | 204 | Sets ride status to `INACTIVE` |
| `POST` | `/rides/search` | Public | `RideSearchRequestDTO` | 200 `RideSearchResultDTO[]` | |
| `POST` | `/rides/{id}/reserve` | User | `ReserveRideRequestDTO` | 201 `{ "bookingId": number }` | |
| `GET` | `/rides/me` | User | — | 200 `MyRidesResponseDTO` | |

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
  `rideStops: [{ id, type, stopOrder, cumulativePricePerSeat, departsAt }]`. Locations are distinct,
  times are at least one minute apart, the origin price is `0`, and later cumulative prices strictly increase.
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
- **Status**: `ACTIVE`, `INACTIVE`
- **TimeWindow**: `BEFORE_8`, `8_12`, `12_18`, `AFTER_18`

## Contract Gotchas & Gaps
- `POST /login` returns `{ token, user }` in the response body despite the client being cookie-only.
- Review endpoints referenced in frontend code do not exist in the backend.
