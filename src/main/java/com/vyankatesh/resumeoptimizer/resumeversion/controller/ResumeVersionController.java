package com.vyankatesh.resumeoptimizer.resumeversion.controller;

import com.vyankatesh.resumeoptimizer.resumeversion.entity.ResumeVersion;
import com.vyankatesh.resumeoptimizer.resumeversion.service.ResumeVersionService;
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

    @PostMapping
    public ResumeVersion saveVersion(@RequestBody ResumeVersion resumeVersion) {
        return resumeVersionService.saveVersion(resumeVersion);
    }

    @GetMapping("/{userEmail}")
    public List<ResumeVersion> getUserVersions(@PathVariable String userEmail) {
        return resumeVersionService.getUserVersions(userEmail);
    }

    @GetMapping("/version/{id}")
    public ResumeVersion getVersionById(@PathVariable Long id) {
        return resumeVersionService.getVersionById(id);
    }

    @DeleteMapping("/{id}")
    public String deleteVersion(@PathVariable Long id) {
        resumeVersionService.deleteVersion(id);
        return "Resume version deleted successfully";
    }
}