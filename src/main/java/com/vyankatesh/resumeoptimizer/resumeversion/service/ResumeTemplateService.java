package com.vyankatesh.resumeoptimizer.resumeversion.service;

import com.vyankatesh.resumeoptimizer.resumeversion.entity.ResumeTemplate;
import com.vyankatesh.resumeoptimizer.resumeversion.repository.ResumeTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResumeTemplateService {

    private final ResumeTemplateRepository resumeTemplateRepository;

    public ResumeTemplateService(ResumeTemplateRepository resumeTemplateRepository) {
        this.resumeTemplateRepository = resumeTemplateRepository;
    }

    public List<ResumeTemplate> getTopTemplates() {
        return resumeTemplateRepository.findByActiveTrueOrderByMarketFitScoreDesc();
    }
}