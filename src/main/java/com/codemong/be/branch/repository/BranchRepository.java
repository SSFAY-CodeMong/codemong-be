package com.codemong.be.branch.repository;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.repository.entity.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findTopByRepository_IdOrderByCreatedAtDesc(Long repositoryId);

    List<Branch> findByRepository_IdOrderByCreatedAtAsc(Long repositoryId);

    void deleteByRepository(GithubRepository repository);
}
