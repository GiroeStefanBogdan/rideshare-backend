package com.example.blablacar.repository.user;

import com.example.blablacar.model.user.UserReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<UserReview, Long> {

    Optional<UserReview> findByReviewerIdAndTargetUserId(long reviewerId, long targetUserId);

    @Query("select r from UserReview r where r.targetUser.id = :targetUserId order by r.publishedAt desc")
    List<UserReview> findAllReceivedByTargetUserId(long targetUserId);

    List<UserReview> findAllByReviewerIdOrderByUpdatedAtDesc(long reviewerId);

    @Query("""
            select r from UserReview r
            where r.reviewer.id = :userId or r.targetUser.id = :userId
            """)
    List<UserReview> findAllInvolvingUserId(long userId);
}