package com.example.blablacar.model.location;

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
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Author: AlexandruDicu
 * Since: 4/1/2026
 */
@Immutable
@Entity
@Table(name = "admin_units")
public class AdministrativeUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "osm_id", nullable = false, unique = true, length = 50)
    private String osmId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AdministrativeUnitType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AdministrativeUnit parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<AdministrativeUnit> children;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "population")
    private Long population;

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AdministrativeUnitType getType() { return type; }
    public void setType(AdministrativeUnitType type) { this.type = type; }

    public AdministrativeUnit getParent() { return parent; }
    public void setParent(AdministrativeUnit parent) { this.parent = parent; }

    public List<AdministrativeUnit> getChildren() { return children; }
    public void setChildren(List<AdministrativeUnit> children) { this.children = children; }
}