package com.example.blablacar.model.ride;

import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.location.Street;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "ride_stop",
        uniqueConstraints = {
                @UniqueConstraint(name = "uc_ride_stop_order", columnNames = {"ride_id", "stop_order"})
        }
)
public class RideStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Short version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private AdministrativeUnit administrativeUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "street_id")
    private Street street;

    @Column(name = "stop_order", nullable = false)
    private Byte stopOrder;

    @Column(name = "departs_at")
    private OffsetDateTime departsAt;

    @Column(name = "available_seats", nullable = false)
    private Byte availableSeats;

    @Column(name = "cumulative_price_per_seat", nullable = false)
    private Short cumulativePricePerSeat;

    protected RideStop() {
    }

    public RideStop(final AdministrativeUnit administrativeUnit, final Street street, final Byte stopOrder,
                    final OffsetDateTime departsAt,
                    final Byte availableSeats, final Short cumulativePricePerSeat) {
        this.administrativeUnit = administrativeUnit;
        this.street = street;
        this.stopOrder = stopOrder;
        this.departsAt = departsAt;
        this.availableSeats = availableSeats;
        this.cumulativePricePerSeat = cumulativePricePerSeat;
    }

    public Long getId() {
        return id;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
    }

    public AdministrativeUnit getLocation() {
        return administrativeUnit;
    }

    public void setLocation(AdministrativeUnit administrativeUnit) {
        this.administrativeUnit = administrativeUnit;
    }

    public Byte getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(final Byte availableSeats) {
        this.availableSeats = availableSeats;
    }

    public Street getStreet() {
        return street;
    }

    public OffsetDateTime getDepartsAt() {
        return departsAt;
    }

    public Short getCumulativePricePerSeat() {
        return cumulativePricePerSeat;
    }

    public Byte getStopOrder() {
        return stopOrder;
    }
}
