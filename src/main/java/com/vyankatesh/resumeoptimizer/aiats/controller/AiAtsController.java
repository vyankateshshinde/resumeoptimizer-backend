package com.vyankatesh.resumeoptimizer.aiats.controller;

import com.vyankatesh.resumeoptimizer.aiats.dto.AiAtsRequest;
import com.vyankatesh.resumeoptimizer.aiats.dto.AiAtsResponse;
import com.vyankatesh.resumeoptimizer.aiats.service.AiAtsService;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai-ats")
@CrossOrigin(origins = "http://localhost:5173")
public class AiAtsController {

    private final AiAtsService aiAtsService;
    private final ResumeRepository resumeRepository;

    public AiAtsController(
            AiAtsService aiAtsService,
            ResumeRepository resumeRepository
    ) {
        this.aiAtsService = aiAtsService;
        this.resumeRepository = resumeRepository;
    }

    @PostMapping("/analyze")
    public AiAtsResponse analyzeResume(
            @RequestBody AiAtsRequest request,
            Authentication authentication
    ) {
        validateResumeOwnership(request.getResumeId(), authentication);

        return aiAtsService.analyzeResume(request);
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

        ResumeEntity resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        if (resume.getEmail() == null
                || !resume.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to use this resume"
            );
        }
    }
}
