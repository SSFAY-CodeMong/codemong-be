package com.codemong.be.user.entity;

import com.codemong.be.role.entity.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.common.aliasing.qual.Unique;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name="app_users")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long snsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    private String email;

    private String profilePath;

    private String name;

    @Column(nullable = false)
    private String htmlUrl;

    @Column(length = 1024)
    private String githubToken;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void updateGithubToken(String encryptToken) {
        this.githubToken = encryptToken;
    }

    public void updateEmail(String email) {
        this.email = email;
    }
}
