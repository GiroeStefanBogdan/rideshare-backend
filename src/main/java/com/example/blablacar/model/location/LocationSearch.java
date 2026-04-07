package com.example.blablacar.model.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.Immutable;

/**
 * Author: AlexandruDicu
 * Since: 4/4/2026
 */
@Entity
@Immutable
@Table(name = "location_search")
public class LocationSearch {

    @Id
    private Long id;

    private String name;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 20)
    private AdministrativeUnitType type;

    public LocationSearch() {
    }

    public LocationSearch(final String name, final String fullName, final AdministrativeUnitType type) {
        this.name = name;
        this.fullName = fullName;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }
}
