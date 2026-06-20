package com.codemong.be.project.repository;

import com.codemong.be.project.entity.ProjectStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectStepRepository extends JpaRepository<ProjectStep, Long> {

    List<ProjectStep> findByProject_IdOrderByStepAsc(Long projectId);

    Optional<ProjectStep> findByProject_IdAndStep(Long projectId, int step);
}
