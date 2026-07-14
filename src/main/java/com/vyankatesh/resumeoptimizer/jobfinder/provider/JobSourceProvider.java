package com.vyankatesh.resumeoptimizer.jobfinder.provider;

import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;

import java.time.LocalDateTime;
import java.util.List;

public interface JobSourceProvider {

    JobSource getSource();

    List<ExternalJobRecord> fetchJobs(LocalDateTime postedAfter);
}
