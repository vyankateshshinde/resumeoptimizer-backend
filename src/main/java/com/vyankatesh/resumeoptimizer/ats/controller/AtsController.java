package com.vyankatesh.resumeoptimizer.ats.controller;

import com.vyankatesh.resumeoptimizer.ats.dto.AiAtsHistoryResponse;
import com.vyankatesh.resumeoptimizer.ats.dto.AtsRequest;
import com.vyankatesh.resumeoptimizer.ats.dto.AtsResponse;
import com.vyankatesh.resumeoptimizer.ats.service.AtsService;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = auth.getName();

        System.out.println("AUTH SET FOR EMAIL = " + email);

        ResumeEntity resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        if (!resume.getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized access to resume");
        }

        return atsService.calculateAtsScore(
                resume.getExtractedText(),
                request.getJobDescription(),
                email,
                resume.getId()
        );
    }

    @GetMapping("/history")
    public List<AiAtsHistoryResponse> getHistory() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = auth.getName();

        System.out.println("AUTH SET FOR EMAIL = " + email);

        return atsService.getHistory(email);
    }

    @GetMapping("/history/latest/{resumeId}")
    public AiAtsHistoryResponse getLatestHistory(
            @PathVariable Long resumeId
    ) {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = auth.getName();

        System.out.println("AUTH SET FOR EMAIL = " + email);
        System.out.println("RESUME ID = " + resumeId);

        return atsService.getLatestHistory(email, resumeId);
    }
}