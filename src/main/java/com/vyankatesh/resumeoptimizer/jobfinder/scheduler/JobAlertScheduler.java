package com.vyankatesh.resumeoptimizer.jobfinder.scheduler;

import com.vyankatesh.resumeoptimizer.jobfinder.service.JobAlertService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobAlertScheduler {

    private final JobAlertService jobAlertService;

    public JobAlertScheduler(JobAlertService jobAlertService) {
        this.jobAlertService = jobAlertService;
    }

    @Scheduled(
            initialDelayString = "${jobfinder.alerts.initial-delay-ms:60000}",
            fixedDelayString = "${jobfinder.alerts.fixed-delay-ms:900000}"
    )
    public void processAlerts() {
        jobAlertService.processEnabledAlerts();
    }
}
