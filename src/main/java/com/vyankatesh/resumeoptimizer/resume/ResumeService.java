package com.vyankatesh.resumeoptimizer.resume;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeParserService resumeParserService;

    public ResumeEntity uploadResume(MultipartFile file, String email) throws IOException {

        String extractedText = resumeParserService.extractText(file);

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
                .orElseThrow(() -> new RuntimeException("Resume not found"));
    }

    public List<ResumeEntity> getMyResumes(String email) {
        return resumeRepository.findByEmailOrderByIdDesc(email);
    }
}