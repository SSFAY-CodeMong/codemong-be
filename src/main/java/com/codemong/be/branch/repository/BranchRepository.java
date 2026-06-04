package com.codemong.be.branch.repository;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.repository.entity.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    void deleteByRepository(GithubRepository repository);
}
