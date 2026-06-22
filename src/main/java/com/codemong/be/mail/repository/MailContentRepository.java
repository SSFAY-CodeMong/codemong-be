package com.codemong.be.mail.repository;

import com.codemong.be.mail.entity.MailContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MailContentRepository extends JpaRepository<MailContent, Long> {

    List<MailContent> findAllByOrderByTypeAscDisplayOrderAsc();

    Optional<MailContent> findByTrackAndSourceFile(String track, String sourceFile);

    @Query(value = "select * from mail_contents order by random() limit 1", nativeQuery = true)
    Optional<MailContent> findRandom();
}
