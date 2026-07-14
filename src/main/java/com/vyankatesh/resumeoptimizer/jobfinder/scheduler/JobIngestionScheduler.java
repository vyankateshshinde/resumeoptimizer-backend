package com.vyankatesh.resumeoptimizer.jobfinder.scheduler;

import com.vyankatesh.resumeoptimizer.jobfinder.service.JobIngestionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobIngestionScheduler {

    private final JobIngestionService jobIngestionService;
    private final int lookbackDays;

    public JobIngestionScheduler(
            JobIngestionService jobIngestionService,
            @Value("${jobfinder.ingestion.lookback-days:7}") int lookbackDays
    ) {
        this.jobIngestionService = jobIngestionService;
        this.lookbackDays = lookbackDays;
    }

    @Scheduled(
            initialDelayString = "${jobfinder.ingestion.initial-delay-ms:5000}",
            fixedDelayString = "${jobfinder.ingestion.fixed-delay-ms:3600000}"
    )
    public void ingestJobs() {
        jobIngestionService.ingestRecentJobs(lookbackDays);
    }
}
