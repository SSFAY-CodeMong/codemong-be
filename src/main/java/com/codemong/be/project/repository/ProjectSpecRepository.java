package com.codemong.be.project.repository;

import com.codemong.be.project.entity.ProjectSpec;
import com.codemong.be.project.entity.ProjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectSpecRepository extends JpaRepository<ProjectSpec, Long> {

    Optional<ProjectSpec> findByProject_IdAndStepAndType(Long projectId, int step, ProjectType type);
}
