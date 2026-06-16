package com.vyankatesh.resumeoptimizer.ats.controller;

import com.vyankatesh.resumeoptimizer.ats.dto.AtsRequest;
import com.vyankatesh.resumeoptimizer.ats.dto.AtsResponse;
import com.vyankatesh.resumeoptimizer.ats.service.AtsService;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ats")
@RequiredArgsConstructor
public class AtsController {

    private final AtsService atsService;
    private final ResumeRepository resumeRepository;

    @PostMapping("/{resumeId}")
    public AtsResponse analyzeResume(
            @PathVariable Long resumeId,
            @RequestBody AtsRequest request
    ) {

        // =========================
        // STEP 1: GET AUTH USER
        // =========================
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = auth.getName();

        System.out.println("=== ATS DEBUG START ===");
        System.out.println("LOGGED IN USER = " + email);

        // =========================
        // STEP 2: FETCH RESUME
        // =========================
        ResumeEntity resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        // =========================
        // STEP 3: SECURITY CHECK
        // =========================
        if (!resume.getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized access to resume");
        }

        System.out.println("RESUME ID = " + resumeId);
        System.out.println("RESUME OWNER = " + resume.getEmail());

        // =========================
        // STEP 4: ATS CALCULATION (UPDATED CALL)
        // =========================
        AtsResponse response = atsService.calculateAtsScore(
                resume.getExtractedText(),
                request.getJobDescription(),
                email,
                resumeId
        );

        System.out.println("FINAL SCORE = " + response.getFinalScore());
        System.out.println("=== ATS DEBUG END ===");

        return response;
    }
}