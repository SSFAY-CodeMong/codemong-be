package com.codemong.be.mail.repository;

import com.codemong.be.mail.entity.MailAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MailAnswerRepository extends JpaRepository<MailAnswer, Long> {

    List<MailAnswer> findTop20ByUser_IdOrderByCreatedAtDesc(Long userId);
}
