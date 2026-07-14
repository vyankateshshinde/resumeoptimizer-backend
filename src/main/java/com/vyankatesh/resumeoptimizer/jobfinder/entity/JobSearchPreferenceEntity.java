package com.vyankatesh.resumeoptimizer.jobfinder.entity;

import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSortOption;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "job_search_preferences",
        indexes = {
                @Index(name = "idx_job_pref_user", columnList = "user_email"),
                @Index(name = "idx_job_pref_updated", columnList = "updated_at")
        }
)
public class JobSearchPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "job_search_preference_titles",
            joinColumns = @JoinColumn(name = "preference_id")
    )
    @OrderColumn(name = "title_order")
    @Column(name = "job_title", nullable = false, length = 120)
    private List<String> jobTitles = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "job_search_preference_locations",
            joinColumns = @JoinColumn(name = "preference_id")
    )
    @OrderColumn(name = "location_order")
    @Column(name = "location", nullable = false, length = 120)
    private List<String> locations = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "job_search_preference_work_arrangements",
            joinColumns = @JoinColumn(name = "preference_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "work_arrangement", nullable = false, length = 30)
    private Set<WorkArrangement> workArrangements = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "job_search_preference_employment_types",
            joinColumns = @JoinColumn(name = "preference_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    private Set<EmploymentType> employmentTypes = new HashSet<>();

    @Column(name = "experience_years", precision = 5, scale = 2)
    private BigDecimal experienceYears;

    @Column(name = "posted_within_days", nullable = false)
    private Integer postedWithinDays = 7;

    @Column(name = "minimum_salary", precision = 15, scale = 2)
    private BigDecimal minimumSalary;

    @Column(name = "minimum_match_percentage", nullable = false)
    private Integer minimumMatchPercentage = 60;

    @Enumerated(EnumType.STRING)
    @Column(name = "sort_by", nullable = false, length = 40)
    private JobSortOption sortBy = JobSortOption.BEST_MATCH;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public List<String> getJobTitles() {
        return jobTitles;
    }

    public void setJobTitles(List<String> jobTitles) {
        this.jobTitles = jobTitles == null ? new ArrayList<>() : new ArrayList<>(jobTitles);
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations == null ? new ArrayList<>() : new ArrayList<>(locations);
    }

    public Set<WorkArrangement> getWorkArrangements() {
        return workArrangements;
    }

    public void setWorkArrangements(Set<WorkArrangement> workArrangements) {
        this.workArrangements = workArrangements == null ? new HashSet<>() : new HashSet<>(workArrangements);
    }

    public Set<EmploymentType> getEmploymentTypes() {
        return employmentTypes;
    }

    public void setEmploymentTypes(Set<EmploymentType> employmentTypes) {
        this.employmentTypes = employmentTypes == null ? new HashSet<>() : new HashSet<>(employmentTypes);
    }

    public BigDecimal getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(BigDecimal experienceYears) {
        this.experienceYears = experienceYears;
    }

    public Integer getPostedWithinDays() {
        return postedWithinDays;
    }

    public void setPostedWithinDays(Integer postedWithinDays) {
        this.postedWithinDays = postedWithinDays;
    }

    public BigDecimal getMinimumSalary() {
        return minimumSalary;
    }

    public void setMinimumSalary(BigDecimal minimumSalary) {
        this.minimumSalary = minimumSalary;
    }

    public Integer getMinimumMatchPercentage() {
        return minimumMatchPercentage;
    }

    public void setMinimumMatchPercentage(Integer minimumMatchPercentage) {
        this.minimumMatchPercentage = minimumMatchPercentage;
    }

    public JobSortOption getSortBy() {
        return sortBy;
    }

    public void setSortBy(JobSortOption sortBy) {
        this.sortBy = sortBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
