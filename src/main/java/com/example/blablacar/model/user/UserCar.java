package com.example.blablacar.model.user;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * Author: AlexandruDicu
 * Since: 24.06.2025
 */
@Entity
@Table(name = "user_cars")
public class UserCar implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String brand;

    @Column(nullable = false, length = 20)
    private String model;

    @Column(nullable = false, length = 30)
    private String color;

    @Column(nullable = false)
    private int year;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Column(name = "number_of_seats", nullable = false)
    private int numberOfSeats;

    public long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getColor() {
        return color;
    }

    public int getYear() {
        return year;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public int getNumberOfSeats() {
        return numberOfSeats;
    }

    @JsonProperty("userId")
    public long getUserId(){
        return user != null ? user.getId() : 0L;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public void setNumberOfSeats(int numberOfSeats) {
        this.numberOfSeats = numberOfSeats;
    }

    public UserCar(User user, String brand, String model, String color, int year, String licensePlate, int numberOfSeats) {
        this.user = user;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.year = year;
        this.licensePlate = licensePlate;
        this.numberOfSeats = numberOfSeats;
    }

    public UserCar() {
    }
}
