package com.vyankatesh.resumeoptimizer.resumeversion.service;

import com.vyankatesh.resumeoptimizer.ats.repository.AtsHistoryRepository;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeComparisonRequest;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeComparisonResponse;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionRequest;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionResponse;
import com.vyankatesh.resumeoptimizer.resumeversion.entity.ResumeVersion;
import com.vyankatesh.resumeoptimizer.resumeversion.repository.ResumeVersionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResumeVersionService {

    private final ResumeVersionRepository resumeVersionRepository;
    private final AtsHistoryRepository atsHistoryRepository;

    public ResumeVersionService(
            ResumeVersionRepository resumeVersionRepository,
            AtsHistoryRepository atsHistoryRepository
    ) {
        this.resumeVersionRepository = resumeVersionRepository;
        this.atsHistoryRepository = atsHistoryRepository;
    }

    public ResumeVersionResponse saveVersion(
            ResumeVersionRequest request,
            String userEmail
    ) {
        if (request.getResumeId() == null) {
            throw new RuntimeException("Resume ID is required");
        }

        if (request.getFullResumeText() == null || request.getFullResumeText().isBlank()) {
            throw new RuntimeException("Resume content is required");
        }

        ResumeVersion version = new ResumeVersion();

        version.setResumeId(request.getResumeId());
        version.setUserEmail(userEmail);
        version.setVersionName(
                request.getVersionName() == null || request.getVersionName().isBlank()
                        ? "Untitled Resume Version"
                        : request.getVersionName()
        );
        version.setTemplateName(
                request.getTemplateName() == null || request.getTemplateName().isBlank()
                        ? "ATS Professional"
                        : request.getTemplateName()
        );
        version.setJobDescription(normalizeJobDescription(request.getJobDescription()));
        version.setFullResumeText(request.getFullResumeText());
        version.setProfessionalSummary(request.getProfessionalSummary());
        version.setSkills(request.getSkills());
        version.setExperienceBullets(request.getExperienceBullets());
        version.setProjectBullets(request.getProjectBullets());
        version.setEducation(request.getEducation());

        version.setAtsScore(
                resolveAtsScore(
                        userEmail,
                        request.getResumeId(),
                        request.getJobDescription()
                )
        );

        ResumeVersion saved = resumeVersionRepository.save(version);

        return mapToResponse(saved);
    }

    public List<ResumeVersionResponse> getUserVersions(String userEmail) {
        return resumeVersionRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ResumeVersionResponse getVersionById(Long id) {
        ResumeVersion version = resumeVersionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Resume version not found with id: " + id
                ));

        return mapToResponse(version);
    }

    public ResumeVersionResponse duplicateVersion(Long id) {
        ResumeVersion existing = resumeVersionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Resume version not found with id: " + id
                ));

        ResumeVersion copy = new ResumeVersion();

        copy.setResumeId(existing.getResumeId());
        copy.setUserEmail(existing.getUserEmail());
        copy.setVersionName(existing.getVersionName() + " - Copy");
        copy.setTemplateName(existing.getTemplateName());
        copy.setJobDescription(existing.getJobDescription());
        copy.setFullResumeText(existing.getFullResumeText());
        copy.setProfessionalSummary(existing.getProfessionalSummary());
        copy.setSkills(existing.getSkills());
        copy.setExperienceBullets(existing.getExperienceBullets());
        copy.setProjectBullets(existing.getProjectBullets());
        copy.setEducation(existing.getEducation());
        copy.setAtsScore(existing.getAtsScore());

        ResumeVersion savedCopy = resumeVersionRepository.save(copy);

        return mapToResponse(savedCopy);
    }

    public ResumeComparisonResponse compareVersions(
            ResumeComparisonRequest request
    ) {
        if (request.getVersionId1() == null || request.getVersionId2() == null) {
            throw new RuntimeException("Both version IDs are required");
        }

        if (request.getVersionId1().equals(request.getVersionId2())) {
            throw new RuntimeException("Please select two different resume versions");
        }

        ResumeVersion version1 = resumeVersionRepository.findById(request.getVersionId1())
                .orElseThrow(() -> new RuntimeException(
                        "Version 1 not found with id: " + request.getVersionId1()
                ));

        ResumeVersion version2 = resumeVersionRepository.findById(request.getVersionId2())
                .orElseThrow(() -> new RuntimeException(
                        "Version 2 not found with id: " + request.getVersionId2()
                ));

        List<String> skills1 = splitSkills(version1.getSkills());
        List<String> skills2 = splitSkills(version2.getSkills());

        List<String> addedSkills = new ArrayList<>(skills2);
        addedSkills.removeAll(skills1);

        List<String> removedSkills = new ArrayList<>(skills1);
        removedSkills.removeAll(skills2);

        int version1Score = version1.getAtsScore();
        int version2Score = version2.getAtsScore();

        ResumeComparisonResponse response = new ResumeComparisonResponse();

        response.setVersion1Name(version1.getVersionName());
        response.setVersion2Name(version2.getVersionName());
        response.setAtsScoreDifference(version2Score - version1Score);
        response.setAddedSkills(addedSkills);
        response.setRemovedSkills(removedSkills);

        response.setComparisonSummary(
                "Compared " + version1.getVersionName()
                        + " with "
                        + version2.getVersionName()
                        + ". ATS score difference is "
                        + (version2Score - version1Score)
                        + "%."
        );

        return response;
    }

    public void deleteVersion(Long id) {
        if (!resumeVersionRepository.existsById(id)) {
            throw new RuntimeException(
                    "Resume version not found with id: " + id
            );
        }

        resumeVersionRepository.deleteById(id);
    }

    private int resolveAtsScore(
            String userEmail,
            Long resumeId,
            String jobDescription
    ) {
        if (userEmail == null || userEmail.isBlank() || resumeId == null) {
            return 0;
        }

        String normalizedJobDescription = normalizeJobDescription(jobDescription);

        if (!normalizedJobDescription.isBlank()) {
            return atsHistoryRepository
                    .findTopByEmailAndResumeIdAndJobDescriptionOrderByCreatedAtDesc(
                            userEmail,
                            resumeId,
                            normalizedJobDescription
                    )
                    .map(history -> Math.max(0, Math.min(100, history.getFinalScore())))
                    .orElse(0);
        }

        return atsHistoryRepository
                .findTopByEmailAndResumeIdOrderByCreatedAtDesc(userEmail, resumeId)
                .map(history -> Math.max(0, Math.min(100, history.getFinalScore())))
                .orElse(0);
    }

    private String normalizeJobDescription(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private List<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return new ArrayList<>();
        }

        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .collect(Collectors.toList());
    }

    private ResumeVersionResponse mapToResponse(ResumeVersion version) {
        return new ResumeVersionResponse(
                version.getId(),
                version.getResumeId(),
                version.getVersionName(),
                version.getTemplateName(),
                version.getFullResumeText(),
                version.getProfessionalSummary(),
                version.getSkills(),
                version.getExperienceBullets(),
                version.getProjectBullets(),
                version.getEducation(),
                version.getAtsScore(),
                version.getCreatedAt()
        );
    }
}
