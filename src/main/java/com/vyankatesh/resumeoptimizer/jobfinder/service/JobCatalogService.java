package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.vyankatesh.resumeoptimizer.jobfinder.entity.JobListingEntity;
import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSearchCriteria;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;
import com.vyankatesh.resumeoptimizer.jobfinder.repository.JobListingRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class JobCatalogService {

    private final JobListingRepository jobListingRepository;

    public JobCatalogService(
            JobListingRepository jobListingRepository
    ) {
        this.jobListingRepository =
                jobListingRepository;
    }

    public List<JobListingEntity> findCandidates(
            JobSearchCriteria criteria,
            int maximumCandidates
    ) {
        Specification<JobListingEntity> specification =
                buildSpecification(criteria);

        PageRequest pageRequest = PageRequest.of(
                0,
                Math.max(
                        1,
                        Math.min(
                                maximumCandidates,
                                500
                        )
                ),
                Sort.by(
                        Sort.Direction.DESC,
                        "postedAt"
                )
        );

        return jobListingRepository
                .findAll(
                        specification,
                        pageRequest
                )
                .getContent();
    }

    private Specification<JobListingEntity>
    buildSpecification(
            JobSearchCriteria criteria
    ) {
        return (
                root,
                query,
                criteriaBuilder
        ) -> {
            List<Predicate> predicates =
                    new ArrayList<>();

            predicates.add(
                    criteriaBuilder.isTrue(
                            root.get("active")
                    )
            );

            if (criteria.postedAfter() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("postedAt"),
                                criteria.postedAfter()
                        )
                );
            }

            if (criteria.jobTitles() != null
                    && !criteria.jobTitles().isEmpty()) {

                List<Predicate> desiredTitlePredicates =
                        new ArrayList<>();

                for (String desiredTitle :
                        criteria.jobTitles()) {

                    if (desiredTitle == null
                            || desiredTitle.isBlank()) {
                        continue;
                    }

                    String[] titleTokens =
                            desiredTitle
                                    .toLowerCase(
                                            Locale.ROOT
                                    )
                                    .split("\\s+");

                    List<Predicate> tokenPredicates =
                            new ArrayList<>();

                    for (String token :
                            titleTokens) {

                        if (token.length() < 3) {
                            continue;
                        }

                        tokenPredicates.add(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get(
                                                        "title"
                                                )
                                        ),
                                        "%"
                                                + token
                                                + "%"
                                )
                        );
                    }

                    if (!tokenPredicates.isEmpty()) {
                        desiredTitlePredicates.add(
                                criteriaBuilder.and(
                                        tokenPredicates.toArray(
                                                Predicate[]::new
                                        )
                                )
                        );
                    }
                }

                if (!desiredTitlePredicates.isEmpty()) {
                    predicates.add(
                            criteriaBuilder.or(
                                    desiredTitlePredicates.toArray(
                                            Predicate[]::new
                                    )
                            )
                    );
                }
            }

            if (criteria.locations() != null
                    && !criteria.locations().isEmpty()) {

                List<Predicate> locationPredicates =
                        new ArrayList<>();

                for (String location :
                        criteria.locations()) {

                    if (location == null
                            || location.isBlank()) {
                        continue;
                    }

                    locationPredicates.add(
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            root.get(
                                                    "location"
                                            )
                                    ),
                                    "%"
                                            + location
                                            .toLowerCase(
                                                    Locale.ROOT
                                            )
                                            + "%"
                            )
                    );
                }

                if (criteria.workArrangements() != null
                        && criteria.workArrangements()
                        .contains(
                                WorkArrangement.REMOTE
                        )) {

                    locationPredicates.add(
                            criteriaBuilder.equal(
                                    root.get(
                                            "workArrangement"
                                    ),
                                    WorkArrangement.REMOTE
                            )
                    );
                }

                if (!locationPredicates.isEmpty()) {
                    predicates.add(
                            criteriaBuilder.or(
                                    locationPredicates.toArray(
                                            Predicate[]::new
                                    )
                            )
                    );
                }
            }

            if (criteria.workArrangements() != null
                    && !criteria.workArrangements()
                    .isEmpty()) {

                predicates.add(
                        root.get(
                                "workArrangement"
                        ).in(
                                criteria.workArrangements()
                        )
                );
            }

            if (criteria.employmentTypes() != null
                    && !criteria.employmentTypes()
                    .isEmpty()) {

                predicates.add(
                        root.get(
                                "employmentType"
                        ).in(
                                criteria.employmentTypes()
                        )
                );
            }

            /*
             * Strict experience eligibility.
             *
             * Exclude only when:
             * 1. Requirement type is REQUIRED.
             * 2. Minimum experience is known.
             * 3. Candidate experience is below that minimum.
             *
             * Preferred, ambiguous and unspecified jobs
             * remain available for further matching.
             */
            if (criteria.experienceYears() != null) {

                Predicate requirementTypeIsNull =
                        criteriaBuilder.isNull(
                                root.get(
                                        "experienceRequirementType"
                                )
                        );

                Predicate requirementIsNotStrict =
                        criteriaBuilder.notEqual(
                                root.get(
                                        "experienceRequirementType"
                                ),
                                ExperienceRequirementType.REQUIRED
                        );

                Predicate requiredMinimumIsUnknown =
                        criteriaBuilder.isNull(
                                root.get(
                                        "minimumExperience"
                                )
                        );

                Predicate candidateMeetsRequiredMinimum =
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get(
                                        "minimumExperience"
                                ),
                                criteria.experienceYears()
                        );

                Predicate nonStrictOrUnknownRequirement =
                        criteriaBuilder.or(
                                requirementTypeIsNull,
                                requirementIsNotStrict
                        );

                Predicate strictRequirementIsSatisfied =
                        criteriaBuilder.or(
                                requiredMinimumIsUnknown,
                                candidateMeetsRequiredMinimum
                        );

                predicates.add(
                        criteriaBuilder.or(
                                nonStrictOrUnknownRequirement,
                                strictRequirementIsSatisfied
                        )
                );
            }

            if (criteria.minimumSalary() != null) {
                predicates.add(
                        criteriaBuilder.and(
                                criteriaBuilder.isNotNull(
                                        root.get(
                                                "maximumSalary"
                                        )
                                ),
                                criteriaBuilder.greaterThanOrEqualTo(
                                        root.get(
                                                "maximumSalary"
                                        ),
                                        criteria.minimumSalary()
                                )
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            Predicate[]::new
                    )
            );
        };
    }
}