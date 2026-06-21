package com.codemong.be.mail.repository;

import com.codemong.be.mail.entity.MailSendLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MailSendLogRepository extends JpaRepository<MailSendLog, Long> {

    List<MailSendLog> findTop20ByUser_IdOrderByCreatedAtDesc(Long userId);
}
