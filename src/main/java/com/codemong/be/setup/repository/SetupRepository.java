package com.codemong.be.setup.repository;

import com.codemong.be.setup.entity.Setup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SetupRepository extends JpaRepository<Setup, Long> {
}
