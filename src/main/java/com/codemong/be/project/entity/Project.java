package com.codemong.be.project.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "ENUM('FE', 'BE')"
    )
    private ProjectType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_step", nullable = false, columnDefinition = "INT DEFAULT 5")
    private int maxStep = 5;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
    )
    private LocalDateTime createdAt;

    public Project(String name, ProjectType type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }

    public Project(String name, ProjectType type, String description, int maxStep) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.maxStep = maxStep;
    }

    public void update(String name, ProjectType type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }

    public void update(String name, ProjectType type, String description, int maxStep) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.maxStep = maxStep;
    }
}
