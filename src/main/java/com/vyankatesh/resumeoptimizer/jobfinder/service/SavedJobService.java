package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.SavedJobResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.SavedJobEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.mapper.JobFinderMapper;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobListingRepository;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.SavedJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final JobListingRepository jobListingRepository;
    private final JobFinderMapper mapper;

    public SavedJobService(
            SavedJobRepository savedJobRepository,
            JobListingRepository jobListingRepository,
            JobFinderMapper mapper
    ) {
        this.savedJobRepository = savedJobRepository;
        this.jobListingRepository = jobListingRepository;
        this.mapper = mapper;
    }

    @Transactional
    public SavedJobResponse save(Long jobId, String userEmail) {
        return savedJobRepository.findByUserEmailAndJobListingId(userEmail, jobId)
                .map(mapper::toSavedJobResponse)
                .orElseGet(() -> {
                    JobListingEntity job = jobListingRepository.findById(jobId)
                            .filter(JobListingEntity::isActive)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Job listing not found"
                            ));

                    SavedJobEntity savedJob = new SavedJobEntity();
                    savedJob.setUserEmail(userEmail);
                    savedJob.setJobListing(job);

                    return mapper.toSavedJobResponse(
                            savedJobRepository.save(savedJob)
                    );
                });
    }

    public List<SavedJobResponse> list(String userEmail) {
        return savedJobRepository.findByUserEmailOrderBySavedAtDesc(userEmail)
                .stream()
                .map(mapper::toSavedJobResponse)
                .toList();
    }

    @Transactional
    public void remove(Long jobId, String userEmail) {
        SavedJobEntity savedJob = savedJobRepository
                .findByUserEmailAndJobListingId(userEmail, jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Saved job not found"
                ));

        savedJobRepository.delete(savedJob);
    }
}
