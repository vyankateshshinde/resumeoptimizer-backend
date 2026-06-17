package com.vyankatesh.resumeoptimizer.ai.service;

import com.vyankatesh.resumeoptimizer.ai.dto.AiRecommendationRequest;
import com.vyankatesh.resumeoptimizer.ai.dto.AiRecommendationResponse;
import com.vyankatesh.resumeoptimizer.ai.entity.AiRecommendationEntity;
import com.vyankatesh.resumeoptimizer.ai.repository.AiRecommendationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiRecommendationService {

    private final AiRecommendationRepository aiRecommendationRepository;

    public AiRecommendationService(AiRecommendationRepository aiRecommendationRepository) {
        this.aiRecommendationRepository = aiRecommendationRepository;
    }

    public AiRecommendationResponse generateRecommendation(AiRecommendationRequest request) {

        String summary = "Improve your professional summary by highlighting Java, Spring Boot, REST APIs, React.js, MySQL, and real-world project experience.";

        String skills = "Add or highlight skills such as Java 17, Spring Boot, Spring Security, JWT, REST APIs, Hibernate, JPA, React.js, MySQL, Docker, and Microservices.";

        String projects = "Enhance your project description by mentioning business impact, secure authentication, resume parsing, ATS score calculation, dashboard analytics, and AI-powered recommendations.";

        String missingSkills = "Docker, Microservices, JUnit, Mockito, Swagger, CI/CD";

        String roadmap = "Week 1: Revise Docker and Spring Boot deployment. Week 2: Practice Microservices and REST API design. Week 3: Add JUnit and Mockito tests. Week 4: Improve frontend dashboard using React and Recharts.";

        AiRecommendationEntity entity = new AiRecommendationEntity();
        entity.setResumeId(request.getResumeId());
        entity.setJobDescription(request.getJobDescription());
        entity.setSummaryRecommendation(summary);
        entity.setSkillRecommendation(skills);
        entity.setProjectRecommendation(projects);
        entity.setMissingSkills(missingSkills);
        entity.setLearningRoadmap(roadmap);

        aiRecommendationRepository.save(entity);

        AiRecommendationResponse response = new AiRecommendationResponse();
        response.setSummaryRecommendation(summary);
        response.setSkillRecommendation(skills);
        response.setProjectRecommendation(projects);
        response.setMissingSkills(missingSkills);
        response.setLearningRoadmap(roadmap);

        return response;
    }

    public List<AiRecommendationEntity> getRecommendationHistory(Long resumeId) {
        return aiRecommendationRepository.findByResumeId(resumeId);
    }
}