package com.codemong.be.mail.repository;

import com.codemong.be.mail.entity.MailCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailCategoryRepository extends JpaRepository<MailCategory, Long> {
}
