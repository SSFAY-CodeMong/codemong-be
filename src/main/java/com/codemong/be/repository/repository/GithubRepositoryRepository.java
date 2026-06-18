package com.codemong.be.repository.repository;

import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.project.entity.Project;
import com.codemong.be.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GithubRepositoryRepository extends JpaRepository<GithubRepository, Long> {

    Optional<GithubRepository> findGithubRepositoryById(Long id);

    @Query("""
            select repository
            from GithubRepository repository
            join fetch repository.user
            join fetch repository.project
            where repository.id = :id
            """)
    Optional<GithubRepository> findByIdWithUserAndProject(@Param("id") Long id);
    
    Optional<GithubRepository> findTopByUserAndProjectOrderByCreatedAtDescIdDesc(User user, Project project);

    List<GithubRepository> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
