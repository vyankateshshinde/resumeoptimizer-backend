package com.vyankatesh.resumeoptimizer.resumebuilder.controller;

import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderRequest;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderResponse;
import com.vyankatesh.resumeoptimizer.resumebuilder.entity.ResumeBuilderHistory;
import com.vyankatesh.resumeoptimizer.resumebuilder.service.ResumeBuilderService;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resume-builder")
@CrossOrigin(origins = "http://localhost:5173")
public class ResumeBuilderController {

    private final ResumeBuilderService resumeBuilderService;

    public ResumeBuilderController(ResumeBuilderService resumeBuilderService) {
        this.resumeBuilderService = resumeBuilderService;
    }

    @PostMapping("/generate")
    public ResumeBuilderResponse generateResume(
            @RequestBody ResumeBuilderRequest request,
            Authentication authentication
    ) {
        return resumeBuilderService.generateResume(
                request,
                authentication.getName()
        );
    }

    @GetMapping("/history")
    public List<ResumeBuilderHistory> getHistory(
            Authentication authentication
    ) {
        return resumeBuilderService.getHistory(authentication.getName());
    }

    @PostMapping("/history/{historyId}/save-version")
    public ResumeVersionResponse saveHistoryAsVersion(
            @PathVariable Long historyId,
            Authentication authentication
    ) {
        return resumeBuilderService.saveHistoryAsVersion(
                historyId,
                authentication.getName()
        );
    }
}