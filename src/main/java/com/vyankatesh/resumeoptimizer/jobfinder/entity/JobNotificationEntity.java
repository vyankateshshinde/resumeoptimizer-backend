package com.vyankatesh.resumeoptimizer.jobfinder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_job_notification_alert_listing",
                columnNames = {"alert_subscription_id", "job_listing_id"}
        ),
        indexes = {
                @Index(name = "idx_job_notification_user", columnList = "user_email, created_at"),
                @Index(name = "idx_job_notification_unread", columnList = "user_email, read_flag")
        }
)
public class JobNotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "alert_subscription_id", nullable = false)
    private JobAlertSubscriptionEntity alertSubscription;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "job_listing_id", nullable = false)
    private JobListingEntity jobListing;

    @Column(name = "match_percentage", nullable = false)
    private int matchPercentage;

    @Column(name = "read_flag", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
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

    public JobAlertSubscriptionEntity getAlertSubscription() {
        return alertSubscription;
    }

    public void setAlertSubscription(JobAlertSubscriptionEntity alertSubscription) {
        this.alertSubscription = alertSubscription;
    }

    public JobListingEntity getJobListing() {
        return jobListing;
    }

    public void setJobListing(JobListingEntity jobListing) {
        this.jobListing = jobListing;
    }

    public int getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(int matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
