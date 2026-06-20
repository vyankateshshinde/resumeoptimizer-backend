package com.vyankatesh.resumeoptimizer.resume;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        try {
            ResumeEntity savedResume = resumeService.uploadResume(file, email);
            return ResponseEntity.ok(savedResume);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/my-resume")
    public ResponseEntity<ResumeEntity> getMyResume() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return ResponseEntity.ok(resumeService.getMyResume(email));
    }

    @GetMapping("/my-resumes")
    public ResponseEntity<List<ResumeEntity>> getMyResumes() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return ResponseEntity.ok(resumeService.getMyResumes(email));
    }
}