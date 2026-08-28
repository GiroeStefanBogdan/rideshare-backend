# Domain Guide

Use these terms consistently in code, API documentation, and plans.

## Users and roles

There is one user role for normal accounts: `ROLE_USER`. A normal user can be both a driver and a passenger. `ROLE_ADMIN` gates administrative user operations.

## Ride

A `Ride` is published by a driver and has an ordered list of `RideStop` records, total seats, departure time, price data, and a `Status`.

`Status.ACTIVE` means the ride is available/current. `Status.INACTIVE` means it is no longer active, including a driver cancellation. There is no persisted `COMPLETED` status; past/upcoming is derived from `departureAt`.

Ride creation accepts departures no more than one month in the future. Ride search returns only active rides with available capacity.

## Ride stops and pricing

`RideStop.stopOrder` defines route order. A booking is valid only when `fromStop.stopOrder < toStop.stopOrder`.

Availability is tracked per stop segment. Reserving `n` seats decrements availability for every stop from the pickup stop through the stop before drop-off.

The persisted stop prices are used as cumulative values; the booking fare is `(fromStop.pricePerSeat - toStop.pricePerSeat) * seats`.

## Booking

A `Booking` links one passenger to one ride and stores the selected pickup/drop-off stops, seats, total price, status, and creation time. Booking price is a snapshot of the amount calculated at reservation time.

The current code allows a driver to reserve their own ride for test purposes. Re-enabling the guard is a future business-rule task.

## Time windows

For personal ride history, “past month” means the rolling interval `[now - 1 month, now)`, using the backend clock and the ride’s `departureAt`. Upcoming begins at `now`.

## Known lifecycle gap

The current ride deletion flow marks rides inactive but also deletes ride stops. Bookings reference those stops, so preserving cancelled booking itineraries requires a separate lifecycle fix before relying on inactive history data.
