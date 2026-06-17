package com.codemong.be.feedback.entity;

import com.codemong.be.branch.entity.Branch;
import com.codemong.be.project.entity.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "feedbacks")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    private String content;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Feedback(Branch branch, String content){
        this.branch = branch;
        this.content = content;
    }
}
