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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private AdministrativeUnit administrativeUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "street_id")
    private Street street;
    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "stop_order", nullable = false)
    private Short stopOrder;

    @Column(name = "departs_at")
    private OffsetDateTime departsAt;

    protected RideStop() {
    }

    public RideStop(Ride ride,
                    AdministrativeUnit administrativeUnit,
                    Street street,
                    Short stopOrder,
                    OffsetDateTime departsAt,
                    String note) {
        this.ride = ride;
        this.administrativeUnit = administrativeUnit;
        this.street = street;
        this.stopOrder = stopOrder;
        this.departsAt = departsAt;
        this.note = note;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Short getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(Short stopOrder) {
        this.stopOrder = stopOrder;
    }

    public OffsetDateTime getDepartsAt() {
        return departsAt;
    }

    public void setDepartsAt(OffsetDateTime departsAt) {
        this.departsAt = departsAt;
    }

    public Street getStreet() {
        return street;
    }

    public void setStreet(final Street street) {
        this.street = street;
    }
}