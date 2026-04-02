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
@Entity
@Immutable
@Table(name = "admin_search")
public class AdministrativeUnitSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AdministrativeUnitType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AdministrativeUnitSearch parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<AdministrativeUnitSearch> children;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "priority")
    private int priority;

    @Column(name = "population")
    private long population;

    @Column(name = "search_text")
    private String searchText;

    @Column(name = "full_name")
    private String fullName;

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AdministrativeUnitType getType() { return type; }
    public void setType(AdministrativeUnitType type) { this.type = type; }

    public AdministrativeUnitSearch getParent() { return parent; }
    public void setParent(AdministrativeUnitSearch parent) { this.parent = parent; }

    public List<AdministrativeUnitSearch> getChildren() { return children; }
    public void setChildren(List<AdministrativeUnitSearch> children) { this.children = children; }
}