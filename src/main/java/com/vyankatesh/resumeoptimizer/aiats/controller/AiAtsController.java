package com.vyankatesh.resumeoptimizer.aiats.controller;

import com.vyankatesh.resumeoptimizer.aiats.dto.AiAtsRequest;
import com.vyankatesh.resumeoptimizer.aiats.dto.AiAtsResponse;
import com.vyankatesh.resumeoptimizer.aiats.service.AiAtsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-ats")
@CrossOrigin(origins = "http://localhost:5173")
public class AiAtsController {

    private final AiAtsService aiAtsService;

    public AiAtsController(AiAtsService aiAtsService) {
        this.aiAtsService = aiAtsService;
    }

    @PostMapping("/analyze")
    public AiAtsResponse analyzeResume(@RequestBody AiAtsRequest request) {
        return aiAtsService.analyzeResume(request);
    }
}