package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSearchCriteria;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobListingRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobCatalogService {

    private final JobListingRepository jobListingRepository;

    public JobCatalogService(JobListingRepository jobListingRepository) {
        this.jobListingRepository = jobListingRepository;
    }

    public List<JobListingEntity> findCandidates(
            JobSearchCriteria criteria,
            int maximumCandidates
    ) {
        Specification<JobListingEntity> specification = buildSpecification(criteria);

        PageRequest pageRequest = PageRequest.of(
                0,
                Math.max(1, Math.min(maximumCandidates, 500)),
                Sort.by(Sort.Direction.DESC, "postedAt")
        );

        return jobListingRepository.findAll(specification, pageRequest).getContent();
    }

    private Specification<JobListingEntity> buildSpecification(JobSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(
                            root.get("postedAt"),
                            criteria.postedAfter()
                    )
            );

            if (!criteria.jobTitles().isEmpty()) {
                List<Predicate> desiredTitlePredicates = new ArrayList<>();

                for (String desiredTitle : criteria.jobTitles()) {
                    String[] titleTokens = desiredTitle.toLowerCase().split("\\s+");
                    List<Predicate> tokenPredicates = new ArrayList<>();

                    for (String token : titleTokens) {
                        if (token.length() < 3) {
                            continue;
                        }

                        tokenPredicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("title")),
                                "%" + token + "%"
                        ));
                    }

                    if (!tokenPredicates.isEmpty()) {
                        desiredTitlePredicates.add(
                                criteriaBuilder.and(tokenPredicates.toArray(Predicate[]::new))
                        );
                    }
                }

                if (!desiredTitlePredicates.isEmpty()) {
                    predicates.add(
                            criteriaBuilder.or(
                                    desiredTitlePredicates.toArray(Predicate[]::new)
                            )
                    );
                }
            }

            if (!criteria.locations().isEmpty()) {
                List<Predicate> locationPredicates = new ArrayList<>(
                        criteria.locations().stream()
                                .map(location -> criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("location")),
                                        "%" + location.toLowerCase() + "%"
                                ))
                                .toList()
                );

                if (criteria.workArrangements().contains(WorkArrangement.REMOTE)) {
                    locationPredicates.add(
                            criteriaBuilder.equal(
                                    root.get("workArrangement"),
                                    WorkArrangement.REMOTE
                            )
                    );
                }

                predicates.add(criteriaBuilder.or(locationPredicates.toArray(Predicate[]::new)));
            }

            if (!criteria.workArrangements().isEmpty()) {
                predicates.add(root.get("workArrangement").in(criteria.workArrangements()));
            }

            if (!criteria.employmentTypes().isEmpty()) {
                predicates.add(root.get("employmentType").in(criteria.employmentTypes()));
            }

            if (criteria.experienceYears() != null) {
                BigDecimal maximumAllowedMinimum = criteria.experienceYears().add(BigDecimal.ONE);
                BigDecimal minimumAllowedMaximum = criteria.experienceYears()
                        .subtract(BigDecimal.ONE)
                        .max(BigDecimal.ZERO);

                Predicate minimumExperienceMatches = criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("minimumExperience")),
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("minimumExperience"),
                                maximumAllowedMinimum
                        )
                );

                Predicate maximumExperienceMatches = criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("maximumExperience")),
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("maximumExperience"),
                                minimumAllowedMaximum
                        )
                );

                predicates.add(
                        criteriaBuilder.and(
                                minimumExperienceMatches,
                                maximumExperienceMatches
                        )
                );
            }

            if (criteria.minimumSalary() != null) {
                predicates.add(
                        criteriaBuilder.and(
                                criteriaBuilder.isNotNull(root.get("maximumSalary")),
                                criteriaBuilder.greaterThanOrEqualTo(
                                        root.get("maximumSalary"),
                                        criteria.minimumSalary()
                                )
                        )
                );
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
