package com.vyankatesh.resumeoptimizer.resumeversion.controller;

import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionRequest;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionResponse;
import com.vyankatesh.resumeoptimizer.resumeversion.entity.ResumeVersion;
import com.vyankatesh.resumeoptimizer.resumeversion.service.ResumeVersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resume-version")
public class ResumeVersionController {

    private final ResumeVersionService resumeVersionService;

    public ResumeVersionController(ResumeVersionService resumeVersionService) {
        this.resumeVersionService = resumeVersionService;
    }

    @PostMapping("/save")
    public ResponseEntity<ResumeVersionResponse> saveVersion(
            @RequestBody ResumeVersionRequest request
    ) {
        ResumeVersionResponse response = resumeVersionService.saveVersion(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{resumeId}")
    public ResponseEntity<List<ResumeVersion>> getVersionsByResumeId(
            @PathVariable Long resumeId
    ) {
        return ResponseEntity.ok(resumeVersionService.getVersionsByResumeId(resumeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeVersion> getVersionById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(resumeVersionService.getVersionById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteVersion(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(resumeVersionService.deleteVersion(id));
    }
}

