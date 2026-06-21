package com.codemong.be.mail.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "mail_contents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mail_contents_track_source",
                columnNames = {"track", "source_file"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MailContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MailContentType type;

    @Column(nullable = false, length = 20)
    private String track;

    @Column(name = "source_file", nullable = false, length = 100)
    private String sourceFile;

    @Column(name = "display_order", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int displayOrder;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
