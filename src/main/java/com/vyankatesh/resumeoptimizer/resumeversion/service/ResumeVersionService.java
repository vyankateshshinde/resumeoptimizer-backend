package com.vyankatesh.resumeoptimizer.resumeversion.service;

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

    public ResumeVersion saveVersion(ResumeVersion resumeVersion) {
        return resumeVersionRepository.save(resumeVersion);
    }

    public List<ResumeVersion> getUserVersions(String userEmail) {
        return resumeVersionRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    public ResumeVersion getVersionById(Long id) {
        return resumeVersionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume version not found"));
    }

    public void deleteVersion(Long id) {
        resumeVersionRepository.deleteById(id);
    }
}
