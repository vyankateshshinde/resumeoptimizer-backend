package com.vyankatesh.resumeoptimizer.resume;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeParserService resumeParserService;

    public ResumeEntity uploadResume(MultipartFile file, String email) throws IOException {

        // Extract text from PDF
        String extractedText = resumeParserService.extractText(file);

        System.out.println("===== DEBUG START =====");
        System.out.println("FILE NAME: " + file.getOriginalFilename());
        System.out.println("EXTRACTED TEXT: " + extractedText);
        System.out.println("===== DEBUG END =====");

        // Save Resume
        ResumeEntity resume = new ResumeEntity();
        resume.setFileName(file.getOriginalFilename());
        resume.setFileType(file.getContentType());
        resume.setData(file.getBytes());
        resume.setEmail(email);
        resume.setExtractedText(extractedText);

        return resumeRepository.save(resume);
    }

    public ResumeEntity getMyResume(String email) {

        return resumeRepository.findTopByEmailOrderByIdDesc(email)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Resume not found"));
    }
}