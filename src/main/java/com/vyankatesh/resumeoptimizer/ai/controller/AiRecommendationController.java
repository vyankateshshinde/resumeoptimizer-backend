package com.vyankatesh.resumeoptimizer.ai.controller;

import com.vyankatesh.resumeoptimizer.ai.dto.AiRecommendationRequest;
import com.vyankatesh.resumeoptimizer.ai.dto.AiRecommendationResponse;
import com.vyankatesh.resumeoptimizer.ai.entity.AiRecommendationEntity;
import com.vyankatesh.resumeoptimizer.ai.service.AiRecommendationService;
import com.vyankatesh.resumeoptimizer.ai.service.GroqService;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;
    private final GroqService groqService;
    private final ResumeRepository resumeRepository;

    public AiRecommendationController(
            AiRecommendationService aiRecommendationService,
            GroqService groqService,
            ResumeRepository resumeRepository
    ) {
        this.aiRecommendationService = aiRecommendationService;
        this.groqService = groqService;
        this.resumeRepository = resumeRepository;
    }

    @PostMapping("/recommendation")
    public ResponseEntity<AiRecommendationResponse> generateRecommendation(
            @RequestBody AiRecommendationRequest request,
            Authentication authentication
    ) {
        validateResumeOwnership(request.getResumeId(), authentication);

        AiRecommendationResponse response =
                aiRecommendationService.generateRecommendation(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendation/history/{resumeId}")
    public ResponseEntity<List<AiRecommendationEntity>> getRecommendationHistory(
            @PathVariable Long resumeId,
            Authentication authentication
    ) {
        validateResumeOwnership(resumeId, authentication);

        List<AiRecommendationEntity> history =
                aiRecommendationService.getRecommendationHistory(resumeId);

        return ResponseEntity.ok(history);
    }

    @PostMapping("/groq-test")
    public ResponseEntity<String> testGroq(
            @RequestBody AiRecommendationRequest request,
            Authentication authentication
    ) {
        validateResumeOwnership(request.getResumeId(), authentication);

        ResumeEntity resume = resumeRepository.findById(request.getResumeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        String response = groqService.generateRecommendation(
                resume.getExtractedText(),
                request.getJobDescription()
        );

        return ResponseEntity.ok(response);
    }

    private void validateResumeOwnership(Long resumeId, Authentication authentication) {
        if (resumeId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resume id is required"
            );
        }

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not authenticated"
            );
        }

        String email = authentication.getName();

        ResumeEntity resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        if (resume.getEmail() == null
                || !resume.getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to use this resume"
            );
        }
    }
}
