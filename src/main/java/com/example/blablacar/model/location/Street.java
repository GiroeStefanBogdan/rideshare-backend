package com.example.blablacar.model.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Author: AlexandruDicu
 * Since: 4/1/2026
 */
@Entity
@Table(name = "streets")
public class Street {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private AdministrativeUnit administrativeUnit;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "name_norm", nullable = false, length = 150)
    private String nameNormalized;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AdministrativeUnit getLocation() {
        return administrativeUnit;
    }

    public void setLocation(AdministrativeUnit administrativeUnit) {
        this.administrativeUnit = administrativeUnit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }


}
