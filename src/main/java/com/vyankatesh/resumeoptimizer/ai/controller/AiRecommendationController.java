package com.vyankatesh.resumeoptimizer.ai.controller;

import com.vyankatesh.resumeoptimizer.ai.dto.AiRecommendationRequest;
import com.vyankatesh.resumeoptimizer.ai.dto.AiRecommendationResponse;
import com.vyankatesh.resumeoptimizer.ai.entity.AiRecommendationEntity;
import com.vyankatesh.resumeoptimizer.ai.service.AiRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;

    public AiRecommendationController(AiRecommendationService aiRecommendationService) {
        this.aiRecommendationService = aiRecommendationService;
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
}