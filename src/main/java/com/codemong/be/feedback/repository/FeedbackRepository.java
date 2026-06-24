package com.codemong.be.feedback.repository;

import com.codemong.be.feedback.dto.FeedbackDetail;
import com.codemong.be.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Optional<Feedback> findFirstByBranch_IdOrderByCreatedAtDesc(Long branchId);

    @Query("""
            SELECT new com.codemong.be.feedback.dto.FeedbackDetail(
                branch.step,
                feedback.content
            )
            FROM Feedback feedback
            JOIN feedback.branch branch
            WHERE branch.repository.id = :repositoryId
            AND feedback.createdAt = (
                SELECT MAX(latestFeedback.createdAt)
                FROM Feedback latestFeedback
                WHERE latestFeedback.branch.id = branch.id
            )
            ORDER BY branch.step ASC
            """)
    List<FeedbackDetail> findLatestDetailsByRepositoryId(@Param("repositoryId") Long repositoryId);
}
