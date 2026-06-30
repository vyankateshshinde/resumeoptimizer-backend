package com.vyankatesh.resumeoptimizer.resumeversion.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_versions")
public class ResumeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long resumeId;

    private String userEmail;
    private String versionName;
    private String templateName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String jobDescription;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String fullResumeText;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String professionalSummary;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String skills;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String experienceBullets;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String projectBullets;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String education;

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

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
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

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getFullResumeText() {
        return fullResumeText;
    }

    public void setFullResumeText(String fullResumeText) {
        this.fullResumeText = fullResumeText;
    }

    public String getProfessionalSummary() {
        return professionalSummary;
    }

    public void setProfessionalSummary(String professionalSummary) {
        this.professionalSummary = professionalSummary;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getExperienceBullets() {
        return experienceBullets;
    }

    public void setExperienceBullets(String experienceBullets) {
        this.experienceBullets = experienceBullets;
    }

    public String getProjectBullets() {
        return projectBullets;
    }

    public void setProjectBullets(String projectBullets) {
        this.projectBullets = projectBullets;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
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
