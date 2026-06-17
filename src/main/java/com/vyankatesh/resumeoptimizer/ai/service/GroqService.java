package com.vyankatesh.resumeoptimizer.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateRecommendation(String resumeText, String jobDescription) {

        try {
            RestTemplate restTemplate = new RestTemplate();

            String prompt = """
                    You are an expert ATS resume optimizer.

                    Return response in this exact format only:

                    Summary Recommendation:
                    Skill Recommendation:
                    Project Recommendation:
                    Missing Skills:
                    Learning Roadmap:

                    Resume Text:
                    %s

                    Job Description:
                    %s
                    """.formatted(resumeText, jobDescription);

            String requestBody = objectMapper.writeValueAsString(
                    new GroqRequest(
                            model,
                            new Message[]{
                                    new Message("system", "You are an expert resume optimization assistant."),
                                    new Message("user", prompt)
                            },
                            0.4
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            return root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to call Groq API: " + e.getMessage());
        }
    }

    private record GroqRequest(
            String model,
            Message[] messages,
            double temperature
    ) {
    }

    private record Message(
            String role,
            String content
    ) {
    }
}
