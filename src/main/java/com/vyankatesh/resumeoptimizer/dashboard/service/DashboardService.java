package com.vyankatesh.resumeoptimizer.dashboard.service;

import com.vyankatesh.resumeoptimizer.ats.repository.AtsHistoryRepository;
import com.vyankatesh.resumeoptimizer.dashboard.dto.DashboardResponse;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ResumeRepository resumeRepository;
    private final AtsHistoryRepository atsHistoryRepository;

    public DashboardResponse getDashboardData(String email) {

        long totalResumes =
                resumeRepository.countByEmail(email);

        long totalAnalyses =
                atsHistoryRepository.countByEmail(email);

        Integer highestScore =
                atsHistoryRepository.getHighestScore(email);

        Double averageScore =
                atsHistoryRepository.getAverageScore(email);

        if (highestScore == null) {
            highestScore = 0;
        }

        if (averageScore == null) {
            averageScore = 0.0;
        }

        return new DashboardResponse(
                totalResumes,
                totalAnalyses,
                highestScore,
                Math.round(averageScore * 100.0) / 100.0
        );
    }
}