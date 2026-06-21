package com.codemong.be.mail.entity;

import com.codemong.be.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mail_send_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MailSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private MailQuestion question;

    @Column(name = "mail_type", nullable = false, length = 50)
    private String mailType;

    @Column(nullable = false, length = 200)
    private String recipient;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 1000)
    private String message;

    @Column(name = "content_title", length = 200)
    private String contentTitle;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MailSendLog(User user, MailQuestion question, String mailType, String recipient, boolean success, String message) {
        this.user = user;
        this.question = question;
        this.mailType = mailType;
        this.recipient = recipient;
        this.success = success;
        this.message = message;
    }

    public MailSendLog(User user, MailQuestion question, String mailType, String recipient, boolean success, String message, String contentTitle) {
        this(user, question, mailType, recipient, success, message);
        this.contentTitle = contentTitle;
    }
}
