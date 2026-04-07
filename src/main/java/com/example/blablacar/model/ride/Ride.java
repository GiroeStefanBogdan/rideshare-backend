package com.example.blablacar.model.ride;

import com.example.blablacar.model.enums.Status;
import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.user.User;
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

    @Column(name = "seats_total", nullable = false)
    private Byte seatsTotal;

    @Column(name = "price_per_seat")
    private Short pricePerSeat;

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
                final List<RideStop> rideStops, final Byte seatsTotal, final Short pricePerSeat,
                final OffsetDateTime departureAt) {
        this.driver = driver;
        this.originAdministrativeUnit = originAdministrativeUnit;
        this.destAdministrativeUnit = destAdministrativeUnit;
        this.rideStops = rideStops;
        this.seatsTotal = seatsTotal;
        this.pricePerSeat = pricePerSeat;
        this.departureAt = departureAt;
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
}