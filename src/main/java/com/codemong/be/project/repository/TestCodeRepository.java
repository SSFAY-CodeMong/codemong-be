package com.codemong.be.project.repository;

import com.codemong.be.project.entity.TestCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TestCodeRepository extends JpaRepository<TestCode, Long> {

    List<TestCode> findByProject_IdAndStepAndMethodNameIn(Long projectId, int step, Collection<String> methodNames);
}
