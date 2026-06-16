package com.codemong.be.feedback.repository;

import com.codemong.be.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Optional<Feedback> findFirstByBranch_IdOrderByCreatedAtDesc(Long branchId);
}
