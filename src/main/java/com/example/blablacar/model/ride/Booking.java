package com.example.blablacar.model.ride;

import com.example.blablacar.model.enums.Status;
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

import java.time.OffsetDateTime;

@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_stop_id", nullable = false)
    private RideStop fromStop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_stop_id", nullable = false)
    private RideStop toStop;

    @Column(name = "seats", nullable = false)
    private Byte seats;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    protected Booking() {
    }

    public Booking(final User passenger, final Ride ride, final RideStop fromStop,
                   final RideStop toStop, final Byte seats, final Integer totalPrice) {
        this.passenger = passenger;
        this.ride = ride;
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.seats = seats;
        this.totalPrice = totalPrice;
        this.status = Status.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public User getPassenger() {
        return passenger;
    }

    public Ride getRide() {
        return ride;
    }

    public RideStop getFromStop() {
        return fromStop;
    }

    public RideStop getToStop() {
        return toStop;
    }

    public Byte getSeats() {
        return seats;
    }

    public Integer getTotalPrice() {
        return totalPrice;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
