package com.vyankatesh.resumeoptimizer.resumeversion.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_versions")
public class ResumeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;
    private String versionName;
    private String templateName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String originalResumeText;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String optimizedResumeText;

    private int atsScore;

    private LocalDateTime createdAt;

    public ResumeVersion() {
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getOriginalResumeText() {
        return originalResumeText;
    }

    public void setOriginalResumeText(String originalResumeText) {
        this.originalResumeText = originalResumeText;
    }

    public String getOptimizedResumeText() {
        return optimizedResumeText;
    }

    public void setOptimizedResumeText(String optimizedResumeText) {
        this.optimizedResumeText = optimizedResumeText;
    }

    public int getAtsScore() {
        return atsScore;
    }

    public void setAtsScore(int atsScore) {
        this.atsScore = atsScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
