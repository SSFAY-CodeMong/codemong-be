package com.codemong.be.mail.repository;

import com.codemong.be.mail.entity.MailQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MailQuestionRepository extends JpaRepository<MailQuestion, Long> {

    @Query(value = "select * from mail_questions order by rand() limit 1", nativeQuery = true)
    Optional<MailQuestion> findRandomQuestion();
}
