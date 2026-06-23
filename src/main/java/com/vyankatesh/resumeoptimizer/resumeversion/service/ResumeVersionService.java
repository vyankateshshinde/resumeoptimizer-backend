package com.vyankatesh.resumeoptimizer.resumeversion.service;

import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionRequest;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionResponse;
import com.vyankatesh.resumeoptimizer.resumeversion.entity.ResumeVersion;
import com.vyankatesh.resumeoptimizer.resumeversion.repository.ResumeVersionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResumeVersionService {

    private final ResumeVersionRepository resumeVersionRepository;

    public ResumeVersionService(ResumeVersionRepository resumeVersionRepository) {
        this.resumeVersionRepository = resumeVersionRepository;
    }

    public ResumeVersionResponse saveVersion(ResumeVersionRequest request, String userEmail) {

        ResumeVersion version = new ResumeVersion();

        version.setResumeId(request.getResumeId());
        version.setUserEmail(userEmail);
        version.setVersionName(request.getVersionName());
        version.setTemplateName(request.getTemplateName());
        version.setFullResumeText(request.getFullResumeText());
        version.setProfessionalSummary(request.getProfessionalSummary());
        version.setSkills(request.getSkills());
        version.setExperienceBullets(request.getExperienceBullets());
        version.setProjectBullets(request.getProjectBullets());
        version.setEducation(request.getEducation());
        version.setAtsScore(request.getAtsScore());

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
                .orElseThrow(() -> new RuntimeException("Resume version not found with id: " + id));

        return mapToResponse(version);
    }

    public ResumeVersionResponse duplicateVersion(Long id) {
        ResumeVersion existing = resumeVersionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume version not found with id: " + id));

        ResumeVersion copy = new ResumeVersion();

        copy.setResumeId(existing.getResumeId());
        copy.setUserEmail(existing.getUserEmail());
        copy.setVersionName(existing.getVersionName() + " - Copy");
        copy.setTemplateName(existing.getTemplateName());
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

    public void deleteVersion(Long id) {
        if (!resumeVersionRepository.existsById(id)) {
            throw new RuntimeException("Resume version not found with id: " + id);
        }

        resumeVersionRepository.deleteById(id);
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