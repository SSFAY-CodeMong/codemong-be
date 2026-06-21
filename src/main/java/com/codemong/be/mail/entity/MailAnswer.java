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
@Table(name = "mail_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MailAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private MailQuestion question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "code_content", columnDefinition = "TEXT")
    private String codeContent;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "recommended_answer", columnDefinition = "TEXT")
    private String recommendedAnswer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MailAnswer(User user, MailQuestion question, String content, String codeContent, int score, String feedback, String recommendedAnswer) {
        this.user = user;
        this.question = question;
        this.content = content;
        this.codeContent = codeContent;
        this.score = score;
        this.feedback = feedback;
        this.recommendedAnswer = recommendedAnswer;
    }
}
