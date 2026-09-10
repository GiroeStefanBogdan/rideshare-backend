package com.example.blablacar.model.ride;

import com.example.blablacar.model.enums.Status;
import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.user.User;
import com.example.blablacar.model.user.UserCar;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "ride")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_location_id", nullable = false)
    private AdministrativeUnit originAdministrativeUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dest_location_id", nullable = false)
    private AdministrativeUnit destAdministrativeUnit;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "ride", cascade = CascadeType.ALL)
    private List<RideStop> rideStops;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    private UserCar car;

    @Column(name = "seats_total", nullable = false)
    private Byte seatsTotal;

    @Column(name = "total_price_per_seat", nullable = false)
    private Short totalPricePerSeat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "departure_at", nullable = false)
    private OffsetDateTime departureAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    protected Ride() {
    }

    public Ride(final User driver, final AdministrativeUnit originAdministrativeUnit,
                final AdministrativeUnit destAdministrativeUnit,
                final List<RideStop> rideStops, final Byte seatsTotal, final Short totalPricePerSeat,
                final OffsetDateTime departureAt, final UserCar car) {
        this.driver = driver;
        this.originAdministrativeUnit = originAdministrativeUnit;
        this.destAdministrativeUnit = destAdministrativeUnit;
        this.rideStops = rideStops;
        this.seatsTotal = seatsTotal;
        this.totalPricePerSeat = totalPricePerSeat;
        this.departureAt = departureAt;
        this.car = car;
        this.createdAt = OffsetDateTime.now();
    }


    public Long getId() {
        return id;
    }

    public User getDriver() {
        return driver;
    }

    public Byte getSeatsTotal() {
        return seatsTotal;
    }

    public void setSeatsTotal(final Byte seatsTotal) {
        this.seatsTotal = seatsTotal;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public List<RideStop> getRideStops() {
        return rideStops;
    }

    public Short getTotalPricePerSeat() {
        return totalPricePerSeat;
    }

    public UserCar getCar() {
        return car;
    }

    public OffsetDateTime getDepartureAt() {
        return departureAt;
    }
}
