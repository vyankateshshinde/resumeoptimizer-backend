package com.vyankatesh.resumeoptimizer.ai.controller;

import com.vyankatesh.resumeoptimizer.ai.dto.AiRecommendationRequest;
import com.vyankatesh.resumeoptimizer.ai.dto.AiRecommendationResponse;
import com.vyankatesh.resumeoptimizer.ai.entity.AiRecommendationEntity;
import com.vyankatesh.resumeoptimizer.ai.service.AiRecommendationService;
import com.vyankatesh.resumeoptimizer.ai.service.GroqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;
    private final GroqService groqService;

    public AiRecommendationController(
            AiRecommendationService aiRecommendationService,
            GroqService groqService
    ) {
        this.aiRecommendationService = aiRecommendationService;
        this.groqService = groqService;
    }

    @PostMapping("/recommendation")
    public ResponseEntity<AiRecommendationResponse> generateRecommendation(
            @RequestBody AiRecommendationRequest request
    ) {
        AiRecommendationResponse response = aiRecommendationService.generateRecommendation(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendation/history/{resumeId}")
    public ResponseEntity<List<AiRecommendationEntity>> getRecommendationHistory(
            @PathVariable Long resumeId
    ) {
        List<AiRecommendationEntity> history =
                aiRecommendationService.getRecommendationHistory(resumeId);

        return ResponseEntity.ok(history);
    }

    @PostMapping("/groq-test")
    public ResponseEntity<String> testGroq(
            @RequestBody AiRecommendationRequest request
    ) {
        String response = groqService.generateRecommendation(
                "Java Spring Boot React MySQL JWT REST API Developer",
                request.getJobDescription()
        );

        return ResponseEntity.ok(response);
    }
}