package com.codemong.be.repository.repository;

import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.project.entity.Project;
import com.codemong.be.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GithubRepositoryRepository extends JpaRepository<GithubRepository, Long> {

    Optional<GithubRepository> findGithubRepositoryById(Long id);
    
    Optional<GithubRepository> findTopByUserAndProjectOrderByCreatedAtDescIdDesc(User user, Project project);
}
