package com.example.blablacar.model.user;


import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Author: AlexandruDicu
 * Since: 24.06.2025
 */
@Entity
@Table(name = "user_reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"target_user_id", "reviewer_id"})
})
public class UserReview implements Serializable {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private String details;

    @Column(nullable = false)
    private LocalDate date;

    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof UserReview that)) {
            return false;
        }

        return score == that.score && targetUser.equals(that.targetUser) && reviewer.equals(that.reviewer)
                && details.equals(that.details) && date.equals(that.date);
    }

    @Override
    public int hashCode() {
        int result = score;
        result = 31 * result + details.hashCode();
        result = 31 * result + date.hashCode();
        return result;
    }
}
