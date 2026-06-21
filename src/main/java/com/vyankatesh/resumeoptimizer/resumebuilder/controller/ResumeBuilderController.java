package com.vyankatesh.resumeoptimizer.resumebuilder.controller;

import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderRequest;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderResponse;
import com.vyankatesh.resumeoptimizer.resumebuilder.service.ResumeBuilderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resume-builder")
@CrossOrigin(origins = "http://localhost:5173")
public class ResumeBuilderController {

    private final ResumeBuilderService resumeBuilderService;

    public ResumeBuilderController(
            ResumeBuilderService resumeBuilderService
    ) {
        this.resumeBuilderService = resumeBuilderService;
    }

    @PostMapping("/generate")
    public ResumeBuilderResponse generateResume(
            @RequestBody ResumeBuilderRequest request
    ) {
        return resumeBuilderService.generateResume(request);
    }
}