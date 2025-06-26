package com.example.blablacar.model.user;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Author: AlexandruDicu
 * Since: 24.06.2025
 */
@Entity
@Table(name = "user_info")
public class UserInfo {

    @Id
    private long id;

    @OneToOne
    @MapsId
    private User user;

    @Column
    private String bio;

    @Column(name = "can_smoke", nullable = false)
    private boolean canSmoke;

    @Column(name = "pet_friendly", nullable = false)
    private boolean petFriendly;
}
