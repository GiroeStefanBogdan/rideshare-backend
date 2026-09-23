package com.example.blablacar.model.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

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

    @Column(name = "rating")
    private Double rating;

    @Column(name = "reviews_count")
    private Integer reviewsCount;

    public UserInfo() {
        // No-arg constructor required by JPA
    }

    public UserInfo(final User user) {
        this.user = user;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public String getBio() {
        return bio;
    }

    public boolean isCanSmoke() {
        return canSmoke;
    }

    public boolean isPetFriendly() {
        return petFriendly;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(final Double rating) {
        this.rating = rating;
    }

    public Integer getReviewsCount() {
        return reviewsCount;
    }

    public void setBio(final String bio) {
        this.bio = bio;
    }

    public void setCanSmoke(final boolean canSmoke) {
        this.canSmoke = canSmoke;
    }

    public void setPetFriendly(final boolean petFriendly) {
        this.petFriendly = petFriendly;
    }

    public void setReviewsCount(final Integer reviewsCount) {
        this.reviewsCount = reviewsCount;
    }

    public void setId(final long id) {
        this.id = id;
    }
}
