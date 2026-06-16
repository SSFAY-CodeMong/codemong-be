package com.codemong.be.setup.entity;

import com.codemong.be.project.entity.Project;
import com.codemong.be.project.entity.ProjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "setup")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Setup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 150)
    private String step;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProjectType type;

    @Column(nullable = false, length = 300)
    private String sha;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
    )
    private LocalDateTime createdAt;

    public Setup(Project project, String step, ProjectType type, String sha) {
        this.project = project;
        this.step = step;
        this.type = type;
        this.sha = sha;
    }
}
