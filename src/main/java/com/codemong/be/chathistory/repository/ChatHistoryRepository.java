package com.codemong.be.chathistory.repository;

import com.codemong.be.chathistory.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    Optional<ChatHistory> findFirstByBranch_IdOrderByCreatedAtDesc(Long branchId);
}
