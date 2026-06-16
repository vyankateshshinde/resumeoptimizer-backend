package com.vyankatesh.resumeoptimizer.resume;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        try {
            resumeService.uploadResume(file, email);
            return ResponseEntity.ok("Resume uploaded successfully");
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

        return ResponseEntity.ok(
                resumeService.getMyResume(email)
        );
    }
}