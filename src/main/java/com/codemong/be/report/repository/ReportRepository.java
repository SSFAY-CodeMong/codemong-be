package com.codemong.be.report.repository;

import com.codemong.be.report.dto.ReportSummary;
import com.codemong.be.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByGithubRepository_IdOrderByCreatedAtDesc(Long repositoryId);

//    @Query(
//            """
//            SELECT new com.blog.backend.dto.LikeUserResponse(
//                u.id,
//                u.username,
//                u.profileImageUrl
//            )
//            FROM Like l
//            JOIN l.user u
//            WHERE l.post.id = :postId
//            """)
//    List<LikeUserResponse> findLikeUserResponsesByPostId(@Param("postId") Long postId);
//



//    Long id,
//    Long repositoryId,
//    String projectName,
//    String content,
//    int score,
//    LocalDateTime createdAt,
//    List<FeedbackDetail> feedbackDetails

//
//    Long step,
//    String content

    @Query("""
            SELECT new com.codemong.be.report.dto.ReportSummary(
                report.id,
                repository.id,
                project.name,
                report.content,
                report.score,
                report.createdAt
            )
            FROM Report report
            JOIN report.githubRepository repository
            JOIN repository.project project
            WHERE repository.user.id = :userId
            ORDER BY report.createdAt DESC
            """)
    List<ReportSummary> getReportList(@Param("userId") Long userId);
}
