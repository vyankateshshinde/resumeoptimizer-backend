package com.vyankatesh.resumeoptimizer.resumeversion.controller;

import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeComparisonRequest;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeComparisonResponse;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionRequest;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionResponse;
import com.vyankatesh.resumeoptimizer.resumeversion.service.ResumeExportService;
import com.vyankatesh.resumeoptimizer.resumeversion.service.ResumeVersionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resume-versions")
@CrossOrigin(origins = "http://localhost:5173")
public class ResumeVersionController {

    private final ResumeVersionService resumeVersionService;
    private final ResumeExportService resumeExportService;

    public ResumeVersionController(
            ResumeVersionService resumeVersionService,
            ResumeExportService resumeExportService
    ) {
        this.resumeVersionService = resumeVersionService;
        this.resumeExportService = resumeExportService;
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

    @PostMapping("/compare")
    public ResumeComparisonResponse compareVersions(
            @RequestBody ResumeComparisonRequest request
    ) {
        return resumeVersionService.compareVersions(request);
    }

    @GetMapping("/{id}/download/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        byte[] file = resumeExportService.exportToPdf(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume-version-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }

    @GetMapping("/{id}/download/docx")
    public ResponseEntity<byte[]> downloadDocx(@PathVariable Long id) {
        byte[] file = resumeExportService.exportToDocx(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume-version-" + id + ".docx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(file);
    }

    @DeleteMapping("/{id}")
    public String deleteVersion(@PathVariable Long id) {
        resumeVersionService.deleteVersion(id);
        return "Resume version deleted successfully";
    }
}