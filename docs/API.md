# REST API

This is the backend wire-contract reference. It describes current controller behavior, not the separate frontend repository’s assumptions. Paths are relative to the backend origin (`http://localhost:8080` in development).

## Authentication model

Requests use the HTTP-only `token` cookie. Protected requests must include cookies. Public routes are `/login`, `/register`, `/auth/logout`, `/error`, `/dashboard`, `/locations/search`, `/rides/search`, `GET /rides/{id}`, and `GET /users/{id}`; `OPTIONS /**` is allowed for CORS preflight. All other routes require authentication. Admin routes additionally require `ROLE_ADMIN`.

## Common errors

Validation failures return HTTP 400 with `ErrorResponseDto`:

```json
{
  "timestamp": "2026-07-14T12:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "field: reason"
}
```

Known domain exceptions use 400, 404, 409, or 422 according to the exception and handler. Error shapes are not fully uniform for every Spring Security or unhandled exception.

## Dashboard

| Method | Path | Auth | Response |
| --- | --- | --- | --- |
| GET | `/dashboard` | Public | `text/plain` username when authenticated; empty/null body otherwise |

## Authentication and users

| Method | Path | Auth | Request | Success |
| --- | --- | --- | --- | --- |
| POST | `/register` | Public | `UserRegistrationRequestDto` | 201, `UserResponseDto` |
| POST | `/login` | Public | `LoginRequest` | 200, `LoginResponse`; sets `token` cookie |
| POST | `/auth/logout` | Public | — | 204; clears `token` cookie |
| GET | `/users` | Admin | — | 200, `UserResponseDto[]` |
| GET | `/users/{id}` | Public | — | 200, `UserPublicProfileDto` |
| GET | `/users/me` | User | — | 200, `UserProfileDto` |
| PATCH | `/users/me` | User | `UpdateUserRequest` | 200, `UserResponseDto` |
| PATCH | `/users/me/password` | User | `LoginRequest` (password is used) | 204 |
| DELETE | `/users/me` | User | — | 204 |
| PATCH | `/admin/users/{id}/role` | Admin | JSON `Role` string | 200, `UserResponseDto` |
| DELETE | `/admin/users/{id}` | Admin | — | 204 |

`UserResponseDto`: `id`, `name`, `email`, `role`, `phoneNumber`, `birthday`, `gender`.

`UserPublicProfileDto`: `id`, `name`, `birthday`, `gender`.

`UserRegistrationRequestDto`: `name`, `email`, `password`, `birthday`, `phoneNumber`, `gender`.

`UpdateUserRequest` fields are nullable partial updates: `name`, `email`, `phoneNumber`, `birthday`, `gender`.

`LoginRequest`: `email`, `password`, `rememberMe`. The current login response includes both a JWT `token` field and a `user` object; the client is intended to use the cookie.

## User cars

| Method | Path | Auth | Request | Success |
| --- | --- | --- | --- | --- |
| POST | `/users/me/cars` | User | `UserCarRequest` | 201, `UserCarResponse` |
| GET | `/users/me/cars` | User | — | 200, `UserCarResponse[]` |
| PATCH | `/users/me/cars/{carId}` | User | `UpdateUserCarRequest` | 200, `UserCarResponse` |
| DELETE | `/users/me/cars/{carId}` | User | — | 204 |

Car request fields: `brand`, `model`, `color`, `year`, `licensePlate`, `numberOfSeats`. The response adds `id` and `userId`.

## Locations

| Method | Path | Auth | Request | Success |
| --- | --- | --- | --- | --- |
| GET | `/locations/search?q={query}` | Public | query string `q` | 200, `LocationResultDTO[]` |

Queries shorter than two trimmed characters return an empty array. A location result contains `id`, `type`, `name`, `fullName`, `latitude`, `longitude`, and `population`.

## Rides and bookings

| Method | Path | Auth | Request | Success |
| --- | --- | --- | --- | --- |
| POST | `/rides` | User | `RideDTO` | 200, numeric ride ID |
| GET | `/rides/{id}` | Public | — | 200, `RideDetailsDTO` |
| PATCH | `/rides/{id}/seats` | User/owner | raw JSON byte seat count | 204 |
| DELETE | `/rides/{id}` | User/owner | — | 204; marks ride inactive |
| POST | `/rides/search` | Public | `RideSearchRequestDTO` | 200, `RideSearchResultDTO[]` |
| POST | `/rides/{id}/reserve` | User | `ReserveRideRequestDTO` | 200, numeric booking ID |
| GET | `/rides/me` | User | — | 200, `MyRidesResponseDTO` |

`RideDTO`: `rideStops` and `seatsTotal`. Each stop contains `id`, `type`, `stopOrder`, `price`, and `departsAt`. Stop order is contiguous, times strictly increase, prices strictly decrease, and the final price is zero.

`RideSearchRequestDTO` requires `fromId`, `fromType`, `toId`, `toType`, `date`, and `seats`; optional filters are `maxDistanceStart`, `maxDistanceEnd`, `maxPrice`, `timeWindow`, `smokingAllowed`, and `petFriendly`.

`RideSearchResultDTO`: `rideId`, `driver`, `seatsAvailable`, `totalPrice`, `startStop`, `endStop`, `distanceToStartKm`, and `distanceToEndKm`. `driver` is `RideDriverDTO`; stops are `RideStopBasicDTO`, including `locationName` and `municipalityName`.

`RideDetailsDTO`: `rideId`, `driver`, `seatsTotal`, and ordered `rideStops`. Detailed stops include `locationName`, `municipalityName`, departure time, available seats, and per-seat price.

`ReserveRideRequestDTO`: `fromStopId`, `toStopId`, and `seats`. The current response is a bare JSON number, not `{ "id": number }`.

`MyRidesResponseDTO` contains `upcomingBookings`, `pastBookings`, `upcomingHostedRides`, and `pastHostedRides`. Booking items contain `bookingId`, `rideId`, `status`, `driver`, `seats`, `totalPrice`, `fromStop`, and `toStop`. Hosted items contain `rideId`, `status`, `seatsTotal`, and ordered `rideStops`.

## Enums

- `Role`: `ROLE_USER`, `ROLE_ADMIN`
- `Gender`: `MALE`, `FEMALE`
- `AuthProvider`: `LOCAL`, `GOOGLE`
- `Status`: `ACTIVE`, `INACTIVE`
- Search time windows: `BEFORE_8`, `8_12`, `12_18`, `AFTER_18`

## Known contract gaps

- The frontend repository still documents review routes that do not currently exist here.
- Schedule and pricing failures return 422 with the standard error response.
