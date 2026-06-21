package com.vyankatesh.resumeoptimizer.prompteditor.controller;

import com.vyankatesh.resumeoptimizer.prompteditor.dto.PromptEditRequest;
import com.vyankatesh.resumeoptimizer.prompteditor.dto.PromptEditResponse;
import com.vyankatesh.resumeoptimizer.prompteditor.service.PromptResumeEditorService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prompt-editor")
@CrossOrigin(origins = "http://localhost:5173")
public class PromptResumeEditorController {

    private final PromptResumeEditorService promptResumeEditorService;

    public PromptResumeEditorController(PromptResumeEditorService promptResumeEditorService) {
        this.promptResumeEditorService = promptResumeEditorService;
    }

    @PostMapping("/refine")
    public PromptEditResponse refineResume(@RequestBody PromptEditRequest request) {
        return promptResumeEditorService.refineResume(request);
    }
}