package com.codemong.be.branch.entity;

import com.codemong.be.repository.entity.GithubRepository;
import com.codemong.be.feedback.entity.Feedback;
import com.codemong.be.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "branches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private GithubRepository repository;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Long step;

    @Column(nullable = false, length = 300)
    private String sha;

    @Column(name = "is_success", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isSuccess = false;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Feedback> feedbacks = new ArrayList<>();

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
    )
    private LocalDateTime createdAt;

    public Branch(User user, GithubRepository repository, String name, Long step, String sha) {
        this.user = user;
        this.repository = repository;
        this.name = name;
        this.step = step;
        this.sha = sha;
    }

    public void markSuccess() {
        this.isSuccess = true;
    }
}
