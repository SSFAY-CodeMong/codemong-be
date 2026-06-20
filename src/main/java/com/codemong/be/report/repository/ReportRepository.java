package com.codemong.be.report.repository;

import com.codemong.be.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByGithubRepository_IdOrderByCreatedAtDesc(Long repositoryId);
}
