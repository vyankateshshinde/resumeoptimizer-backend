package com.vyankatesh.resumeoptimizer.resumeversion.service;

import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionRequest;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionResponse;
import com.vyankatesh.resumeoptimizer.resumeversion.entity.ResumeVersion;
import com.vyankatesh.resumeoptimizer.resumeversion.repository.ResumeVersionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResumeVersionService {

    private final ResumeVersionRepository resumeVersionRepository;

    public ResumeVersionService(ResumeVersionRepository resumeVersionRepository) {
        this.resumeVersionRepository = resumeVersionRepository;
    }

    public ResumeVersionResponse saveVersion(ResumeVersionRequest request) {

        ResumeVersion version = new ResumeVersion();
        version.setResumeId(request.getResumeId());
        version.setVersionName(request.getVersionName());
        version.setSummary(request.getSummary());
        version.setSkills(request.getSkills());
        version.setProjects(request.getProjects());

        ResumeVersion savedVersion = resumeVersionRepository.save(version);

        return mapToResponse(savedVersion);
    }

    public List<ResumeVersion> getVersionsByResumeId(Long resumeId) {
        return resumeVersionRepository.findByResumeId(resumeId);
    }

    public ResumeVersion getVersionById(Long id) {
        return resumeVersionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume version not found with id: " + id));
    }

    public String deleteVersion(Long id) {
        ResumeVersion version = getVersionById(id);
        resumeVersionRepository.delete(version);
        return "Resume version deleted successfully";
    }

    private ResumeVersionResponse mapToResponse(ResumeVersion version) {
        return new ResumeVersionResponse(
                version.getId(),
                version.getResumeId(),
                version.getVersionName(),
                version.getSummary(),
                version.getSkills(),
                version.getProjects(),
                version.getCreatedAt()
        );
    }
}
