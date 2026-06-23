package com.vyankatesh.resumeoptimizer.resumeversion.controller;

import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionRequest;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionResponse;
import com.vyankatesh.resumeoptimizer.resumeversion.service.ResumeVersionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resume-versions")
@CrossOrigin(origins = "http://localhost:5173")
public class ResumeVersionController {

    private final ResumeVersionService resumeVersionService;

    public ResumeVersionController(ResumeVersionService resumeVersionService) {
        this.resumeVersionService = resumeVersionService;
    }

    @PostMapping("/save")
    public ResumeVersionResponse saveVersion(
            @RequestBody ResumeVersionRequest request,
            Authentication authentication
    ) {
        return resumeVersionService.saveVersion(request, authentication.getName());
    }

    @GetMapping("/my-versions")
    public List<ResumeVersionResponse> getMyVersions(Authentication authentication) {
        return resumeVersionService.getUserVersions(authentication.getName());
    }

    @GetMapping("/{id}")
    public ResumeVersionResponse getVersionById(@PathVariable Long id) {
        return resumeVersionService.getVersionById(id);
    }

    @PostMapping("/duplicate/{id}")
    public ResumeVersionResponse duplicateVersion(@PathVariable Long id) {
        return resumeVersionService.duplicateVersion(id);
    }

    @DeleteMapping("/{id}")
    public String deleteVersion(@PathVariable Long id) {
        resumeVersionService.deleteVersion(id);
        return "Resume version deleted successfully";
    }
}