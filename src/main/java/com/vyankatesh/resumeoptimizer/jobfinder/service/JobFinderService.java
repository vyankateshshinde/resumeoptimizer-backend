package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.dto.request.JobFinderSearchRequest;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobDetailsResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobFinderSearchResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.dto.response.JobMatchResponse;
import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.mapper.JobFinderMapper;
import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobMatchResult;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSearchCriteria;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSortOption;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobListingRepository;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class JobFinderService {

    private static final int MAXIMUM_CANDIDATES = 300;

    private final ResumeRepository resumeRepository;
    private final JobListingRepository jobListingRepository;
    private final JobCatalogService jobCatalogService;
    private final JobMatchingService jobMatchingService;
    private final JobFinderMapper mapper;

    public JobFinderService(
            ResumeRepository resumeRepository,
            JobListingRepository jobListingRepository,
            JobCatalogService jobCatalogService,
            JobMatchingService jobMatchingService,
            JobFinderMapper mapper
    ) {
        this.resumeRepository = resumeRepository;
        this.jobListingRepository = jobListingRepository;
        this.jobCatalogService = jobCatalogService;
        this.jobMatchingService = jobMatchingService;
        this.mapper = mapper;
    }

    public JobFinderSearchResponse search(
            JobFinderSearchRequest request,
            String userEmail
    ) {
        ResumeEntity resume = getOwnedResume(request.resumeId(), userEmail);
        JobSearchCriteria criteria = toCriteria(request);

        List<ScoredJob> scoredJobs = scoreCandidates(
                resume.getExtractedText(),
                criteria,
                MAXIMUM_CANDIDATES
        );

        sort(scoredJobs, criteria.sortOption());

        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 20 : request.size();
        int fromIndex = Math.min(page * size, scoredJobs.size());
        int toIndex = Math.min(fromIndex + size, scoredJobs.size());

        List<JobMatchResponse> jobs = scoredJobs.subList(fromIndex, toIndex).stream()
                .map(scoredJob -> mapper.toMatchResponse(
                        scoredJob.job(),
                        scoredJob.match()
                ))
                .toList();

        int totalPages = scoredJobs.isEmpty()
                ? 0
                : (int) Math.ceil(scoredJobs.size() / (double) size);

        return new JobFinderSearchResponse(
                scoredJobs.size(),
                totalPages,
                page,
                size,
                LocalDateTime.now(),
                jobs
        );
    }

    public JobDetailsResponse getJobDetails(Long jobId) {
        JobListingEntity job = jobListingRepository.findById(jobId)
                .filter(JobListingEntity::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job listing not found"
                ));

        return mapper.toDetailsResponse(job);
    }

    public List<ScoredJob> scoreCandidates(
            String resumeText,
            JobSearchCriteria criteria,
            int maximumCandidates
    ) {
        List<JobListingEntity> candidates = jobCatalogService.findCandidates(
                criteria,
                maximumCandidates
        );

        List<ScoredJob> scoredJobs = new ArrayList<>();

        for (JobListingEntity job : candidates) {
            JobMatchResult match = jobMatchingService.calculateMatch(
                    resumeText,
                    criteria,
                    job
            );

            if (match.overallScore() >= criteria.minimumMatchPercentage()) {
                scoredJobs.add(new ScoredJob(job, match));
            }
        }

        return scoredJobs;
    }

    public ResumeEntity getOwnedResume(Long resumeId, String userEmail) {
        ResumeEntity resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        if (resume.getEmail() == null
                || !resume.getEmail().equalsIgnoreCase(userEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot use another user's resume"
            );
        }

        if (resume.getExtractedText() == null
                || resume.getExtractedText().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected resume does not contain extracted text"
            );
        }

        return resume;
    }

    public JobSearchCriteria toCriteria(JobFinderSearchRequest request) {
        int postedWithinDays = request.postedWithinDays() == null
                ? 7
                : request.postedWithinDays();

        return new JobSearchCriteria(
                normalizeStrings(request.jobTitles(), 5),
                normalizeStrings(request.locations(), 10),
                safeWorkArrangements(request.workArrangements()),
                safeEmploymentTypes(request.employmentTypes()),
                request.experienceYears(),
                request.minimumSalary(),
                LocalDateTime.now().minusDays(postedWithinDays),
                request.sortBy() == null
                        ? JobSortOption.BEST_MATCH
                        : request.sortBy(),
                request.minimumMatchPercentage() == null
                        ? 0
                        : request.minimumMatchPercentage()
        );
    }

    public void sort(List<ScoredJob> scoredJobs, JobSortOption option) {
        Comparator<ScoredJob> comparator = switch (option) {
            case NEWEST -> Comparator.comparing(
                    scoredJob -> scoredJob.job().getPostedAt(),
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
            case SALARY_HIGH_TO_LOW -> Comparator.comparing(
                    scoredJob -> salaryForSorting(scoredJob.job()),
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
            case EXPERIENCE_LOW_TO_HIGH -> Comparator.comparing(
                    scoredJob -> scoredJob.job().getMinimumExperience(),
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case BEST_MATCH -> Comparator.comparingInt(
                    (ScoredJob scoredJob) -> scoredJob.match().overallScore()
            ).reversed().thenComparing(
                    (ScoredJob scoredJob) -> scoredJob.job().getPostedAt(),
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        };

        scoredJobs.sort(comparator);
    }

    private BigDecimal salaryForSorting(JobListingEntity job) {
        if (job.getMaximumSalary() != null) {
            return job.getMaximumSalary();
        }
        return job.getMinimumSalary();
    }

    private List<String> normalizeStrings(List<String> values, int limit) {
        if (values == null) {
            return List.of();
        }

        LinkedHashSet<String> uniqueValues = new LinkedHashSet<>();

        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            String normalized = value.trim()
                    .replaceAll("\\s+", " ")
                    .toLowerCase(Locale.ROOT);

            uniqueValues.add(normalized);

            if (uniqueValues.size() >= limit) {
                break;
            }
        }

        return List.copyOf(uniqueValues);
    }

    private Set<WorkArrangement> safeWorkArrangements(
            Set<WorkArrangement> values
    ) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    private Set<EmploymentType> safeEmploymentTypes(
            Set<EmploymentType> values
    ) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    public record ScoredJob(
            JobListingEntity job,
            JobMatchResult match
    ) {
    }
}
