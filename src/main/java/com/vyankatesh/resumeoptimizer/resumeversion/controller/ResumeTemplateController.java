package com.vyankatesh.resumeoptimizer.resumeversion.controller;

import com.vyankatesh.resumeoptimizer.resumeversion.entity.ResumeTemplate;
import com.vyankatesh.resumeoptimizer.resumeversion.service.ResumeTemplateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "http://localhost:5173")
public class ResumeTemplateController {

    private final ResumeTemplateService resumeTemplateService;

    public ResumeTemplateController(ResumeTemplateService resumeTemplateService) {
        this.resumeTemplateService = resumeTemplateService;
    }

    @GetMapping
    public List<ResumeTemplate> getTopTemplates() {
        return resumeTemplateService.getTopTemplates();
    }
}