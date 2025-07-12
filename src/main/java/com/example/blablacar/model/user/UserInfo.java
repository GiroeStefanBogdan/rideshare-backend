package com.example.blablacar.model.user;


import jakarta.persistence.*;

import java.io.Serializable;

/**
 * Author: AlexandruDicu
 * Since: 24.06.2025
 */
@Entity
@Table(name = "user_info")
public class UserInfo implements Serializable {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
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
