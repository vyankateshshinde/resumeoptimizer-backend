package com.vyankatesh.resumeoptimizer.jobfinder.entity;

import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_listings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_job_source_external_id",
                columnNames = {"source", "external_id"}
        ),
        indexes = {
                @Index(name = "idx_job_active_posted", columnList = "active, posted_at"),
                @Index(name = "idx_job_title", columnList = "title"),
                @Index(name = "idx_job_location", columnList = "location"),
                @Index(name = "idx_job_work_arrangement", columnList = "work_arrangement")
        }
)
public class JobListingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 180)
    private String company;

    @Column(length = 180)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_arrangement", nullable = false, length = 30)
    private WorkArrangement workArrangement = WorkArrangement.UNSPECIFIED;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    private EmploymentType employmentType = EmploymentType.OTHER;

    @Column(name = "minimum_experience", precision = 5, scale = 2)
    private BigDecimal minimumExperience;

    @Column(name = "maximum_experience", precision = 5, scale = 2)
    private BigDecimal maximumExperience;

    @Column(name = "minimum_salary", precision = 15, scale = 2)
    private BigDecimal minimumSalary;

    @Column(name = "maximum_salary", precision = 15, scale = 2)
    private BigDecimal maximumSalary;

    @Column(name = "salary_currency", length = 10)
    private String salaryCurrency;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "posted_at", nullable = false)
    private LocalDateTime postedAt;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private JobSource source;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "apply_url", nullable = false, length = 2048)
    private String applyUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (postedAt == null) {
            postedAt = now;
        }
        if (fetchedAt == null) {
            fetchedAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public WorkArrangement getWorkArrangement() {
        return workArrangement;
    }

    public void setWorkArrangement(WorkArrangement workArrangement) {
        this.workArrangement = workArrangement;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public BigDecimal getMinimumExperience() {
        return minimumExperience;
    }

    public void setMinimumExperience(BigDecimal minimumExperience) {
        this.minimumExperience = minimumExperience;
    }

    public BigDecimal getMaximumExperience() {
        return maximumExperience;
    }

    public void setMaximumExperience(BigDecimal maximumExperience) {
        this.maximumExperience = maximumExperience;
    }

    public BigDecimal getMinimumSalary() {
        return minimumSalary;
    }

    public void setMinimumSalary(BigDecimal minimumSalary) {
        this.minimumSalary = minimumSalary;
    }

    public BigDecimal getMaximumSalary() {
        return maximumSalary;
    }

    public void setMaximumSalary(BigDecimal maximumSalary) {
        this.maximumSalary = maximumSalary;
    }

    public String getSalaryCurrency() {
        return salaryCurrency;
    }

    public void setSalaryCurrency(String salaryCurrency) {
        this.salaryCurrency = salaryCurrency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public JobSource getSource() {
        return source;
    }

    public void setSource(JobSource source) {
        this.source = source;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getApplyUrl() {
        return applyUrl;
    }

    public void setApplyUrl(String applyUrl) {
        this.applyUrl = applyUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
