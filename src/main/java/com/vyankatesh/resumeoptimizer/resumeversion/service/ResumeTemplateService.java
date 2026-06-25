package com.vyankatesh.resumeoptimizer.resumeversion.service;

import com.vyankatesh.resumeoptimizer.resumeversion.dto.TemplateRecommendationRequest;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.TemplateRecommendationResponse;
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

    public TemplateRecommendationResponse recommendTemplate(TemplateRecommendationRequest request) {

        List<ResumeTemplate> templates = resumeTemplateRepository.findByActiveTrueOrderByMarketFitScoreDesc();

        if (templates.isEmpty()) {
            throw new RuntimeException("No active resume templates found");
        }

        String input = (
                safe(request.getTargetRole()) + " " +
                        safe(request.getExperienceLevel()) + " " +
                        safe(request.getSkills())
        ).toLowerCase();

        ResumeTemplate recommendedTemplate = templates.get(0);
        String reason = "ATS Professional is recommended because it is the most balanced ATS-friendly template.";

        if (containsAny(input, "java", "spring", "spring boot", "react", "backend", "frontend", "full stack", "software", "developer", "microservices", "devops")) {
            recommendedTemplate = findTemplateByName(templates, "Software Engineer");
            reason = "Software Engineer template is best suited for Java, Full Stack, Backend, Frontend and Technology roles.";
        } else if (containsAny(input, "manager", "lead", "architect", "senior", "executive", "leadership")) {
            recommendedTemplate = findTemplateByName(templates, "Executive Leadership");
            reason = "Executive Leadership template is best suited for senior professionals, team leads, architects and management roles.";
        } else if (containsAny(input, "fresher", "graduate", "campus", "internship", "entry level")) {
            recommendedTemplate = findTemplateByName(templates, "Fresher & Campus");
            reason = "Fresher & Campus template is best suited for freshers, graduates and internship applicants.";
        } else if (containsAny(input, "product", "business analyst", "product owner", "scrum", "agile")) {
            recommendedTemplate = findTemplateByName(templates, "Product & Business");
            reason = "Product & Business template is best suited for product, business analyst and product owner roles.";
        } else if (containsAny(input, "finance", "banking", "consulting", "accounting")) {
            recommendedTemplate = findTemplateByName(templates, "Finance & Consulting");
            reason = "Finance & Consulting template is best suited for finance, banking and consulting careers.";
        } else if (containsAny(input, "marketing", "creative", "content", "design", "branding")) {
            recommendedTemplate = findTemplateByName(templates, "Creative & Marketing");
            reason = "Creative & Marketing template is best suited for marketing, branding, content and design roles.";
        } else if (containsAny(input, "healthcare", "medical", "pharma", "research")) {
            recommendedTemplate = findTemplateByName(templates, "Healthcare & Research");
            reason = "Healthcare & Research template is best suited for healthcare, pharmaceutical and research professionals.";
        }

        return new TemplateRecommendationResponse(
                recommendedTemplate.getTemplateName(),
                reason,
                recommendedTemplate
        );
    }

    private ResumeTemplate findTemplateByName(List<ResumeTemplate> templates, String templateName) {
        return templates.stream()
                .filter(template -> template.getTemplateName().equalsIgnoreCase(templateName))
                .findFirst()
                .orElse(templates.get(0));
    }

    private boolean containsAny(String input, String... keywords) {
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}