package com.codemong.be.process.repository;

import com.codemong.be.process.entity.Process;
import com.codemong.be.repository.entity.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessRepository extends JpaRepository<Process, Long> {

    void deleteByRepository(GithubRepository repository);
}
