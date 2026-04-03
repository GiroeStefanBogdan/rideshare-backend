package com.example.blablacar.model.ride;

import com.example.blablacar.model.enums.Status;
import com.example.blablacar.model.location.AdministrativeUnit;
import com.example.blablacar.model.user.User;
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
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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

    @Column(name = "seats_total", nullable = false)
    private Short seatsTotal;

    @Column(name = "price_per_seat", precision = 8, scale = 2)
    private BigDecimal pricePerSeat;

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

    public Ride(User driver,
                AdministrativeUnit originAdministrativeUnit,
                AdministrativeUnit destAdministrativeUnit,
                Short seatsTotal,
                BigDecimal pricePerSeat,
                Status status,
                OffsetDateTime departureAt) {
        this.driver = driver;
        this.originAdministrativeUnit = originAdministrativeUnit;
        this.destAdministrativeUnit = destAdministrativeUnit;
        this.seatsTotal = seatsTotal;
        this.pricePerSeat = pricePerSeat;
        this.status = status != null ? status : Status.ACTIVE;
        this.departureAt = departureAt;
    }

    public Long getId() {
        return id;
    }

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
    }

    public AdministrativeUnit getOriginLocation() {
        return originAdministrativeUnit;
    }

    public void setOriginLocation(AdministrativeUnit originAdministrativeUnit) {
        this.originAdministrativeUnit = originAdministrativeUnit;
    }

    public AdministrativeUnit getDestLocation() {
        return destAdministrativeUnit;
    }

    public void setDestLocation(AdministrativeUnit destAdministrativeUnit) {
        this.destAdministrativeUnit = destAdministrativeUnit;
    }

    public Short getSeatsTotal() {
        return seatsTotal;
    }

    public void setSeatsTotal(Short seatsTotal) {
        this.seatsTotal = seatsTotal;
    }

    public BigDecimal getPricePerSeat() {
        return pricePerSeat;
    }

    public void setPricePerSeat(BigDecimal pricePerSeat) {
        this.pricePerSeat = pricePerSeat;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public OffsetDateTime getDepartureAt() {
        return departureAt;
    }

    public void setDepartureAt(OffsetDateTime departureAt) {
        this.departureAt = departureAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return Status.ACTIVE.equals(this.status);
    }
}