package com.vyankatesh.resumeoptimizer.resumeversion.config;

import com.vyankatesh.resumeoptimizer.resumeversion.entity.ResumeTemplate;
import com.vyankatesh.resumeoptimizer.resumeversion.repository.ResumeTemplateRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class ResumeTemplateDataLoader {

    private final ResumeTemplateRepository repository;

    public ResumeTemplateDataLoader(ResumeTemplateRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void loadTemplates() {

        if (repository.count() > 0) {
            return;
        }

        repository.save(new ResumeTemplate(
                "ATS Professional",
                "Professional",
                "Best ATS-friendly template for all industries and experience levels.",
                "",
                true,
                true,
                100
        ));

        repository.save(new ResumeTemplate(
                "Software Engineer",
                "Technology",
                "Designed for Java, Full Stack, Backend, Frontend and DevOps professionals.",
                "",
                true,
                true,
                98
        ));

        repository.save(new ResumeTemplate(
                "Executive Leadership",
                "Management",
                "Ideal for Team Leads, Managers, Architects and Senior Professionals.",
                "",
                true,
                true,
                95
        ));

        repository.save(new ResumeTemplate(
                "Fresher & Campus",
                "Entry Level",
                "Optimized for freshers, graduates and internship applicants.",
                "",
                true,
                true,
                94
        ));

        repository.save(new ResumeTemplate(
                "Product & Business",
                "Business",
                "Suitable for Product Managers, Business Analysts and Product Owners.",
                "",
                true,
                true,
                92
        ));

        repository.save(new ResumeTemplate(
                "Finance & Consulting",
                "Finance",
                "Professional format for finance, banking and consulting careers.",
                "",
                true,
                true,
                90
        ));

        repository.save(new ResumeTemplate(
                "Creative & Marketing",
                "Creative",
                "Best for marketing, branding, content creation and design professionals.",
                "",
                true,
                true,
                88
        ));

        repository.save(new ResumeTemplate(
                "Healthcare & Research",
                "Healthcare",
                "Designed for healthcare, pharmaceutical and research professionals.",
                "",
                true,
                true,
                87
        ));

        System.out.println("===== 8 Resume Templates Loaded Successfully =====");
    }
}