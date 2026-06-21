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

        return callGroq(prompt, "You are an expert resume optimization assistant.", 0.4);
    }

    public String generatePromptBasedResumeEdit(String currentResumeText,
                                                String jobDescription,
                                                String userPrompt) {

        String prompt = """
                You are an expert ATS resume editor and resume rewriting assistant.

                Your task:
                Modify the current resume according to the user's prompt and job description.

                Important rules:
                1. Do not add fake skills, tools, companies, education, certifications, or experience.
                2. If the user asks to remove a skill they do not know, remove it from Skills, Summary, Experience, and Projects.
                3. If the user asks to remove weak lines, rewrite or remove generic lines.
                4. If the user asks to add impact metrics, add only realistic wording. Do not invent exact numbers unless the user gives numbers.
                5. If the user gives a metric like 30%%, use that metric naturally.
                6. Keep the resume ATS-friendly.
                7. Keep the resume professional and suitable for the job description.
                8. Preserve the original resume structure as much as possible.
                9. Return only the final updated resume text. Do not add explanation outside resume.

                Current Resume:
                %s

                Job Description:
                %s

                User Prompt:
                %s
                """.formatted(currentResumeText, jobDescription, userPrompt);

        return callGroq(prompt, "You are a strict, honest, ATS-friendly resume editor.", 0.3);
    }

    public String generateAiAtsAnalysis(String resumeText, String jobDescription) {

        String prompt = """
                You are an advanced ATS resume analysis engine.

                Analyze the resume against the job description for any industry:
                IT, healthcare, finance, sales, marketing, education, legal, engineering,
                operations, HR, management, government, and other sectors.

                Return ONLY valid JSON.
                Do not add markdown.
                Do not add explanation outside JSON.

                JSON format:
                {
                  "atsScore": 0,
                  "matchedSkills": [],
                  "missingSkills": [],
                  "strengths": [],
                  "weaknesses": [],
                  "recommendations": []
                }

                Rules:
                1. atsScore must be between 0 and 100.
                2. matchedSkills should include skills, tools, responsibilities, domain knowledge, qualifications, and experience found in both resume and JD.
                3. missingSkills should include important JD requirements missing from resume.
                4. strengths should explain where resume matches well.
                5. weaknesses should explain gaps.
                6. recommendations should suggest resume improvements.
                7. Do not invent experience.
                8. Keep response industry-neutral and ATS-focused.
                9. Return arrays as JSON arrays of strings.
                10. Do not wrap JSON in ```json or markdown.

                Resume:
                %s

                Job Description:
                %s
                """.formatted(resumeText, jobDescription);

        return callGroq(prompt, "You are a strict ATS analysis engine that returns only valid JSON.", 0.2);
    }
    public String generateResumeBuilderContent(String resumeText,
                                               String jobDescription,
                                               String templateName) {

        String prompt = """
            You are an expert resume builder and ATS resume writer.

            Create an optimized resume based on the user's existing resume and job description.

            Return ONLY valid JSON.
            Do not add markdown.
            Do not add explanation outside JSON.

            JSON format:
            {
              "templateName": "",
              "fullResumeText": "",
              "professionalSummary": "",
              "skills": [],
              "experienceBullets": [],
              "projectBullets": [],
              "education": ""
            }

            Rules:
            1. Do not invent fake companies, education, certifications, or experience.
            2. Use only skills and experience supported by the original resume.
            3. Align content with the job description naturally.
            4. Make resume ATS-friendly and professional.
            5. Use strong action verbs.
            6. Keep bullets realistic and recruiter-friendly.
            7. If education is present in resume, include it. If not present, keep education empty.
            8. Return arrays as JSON arrays of strings.
            9. Use the selected template name in templateName.
            10. Do not wrap JSON in ```json or markdown.

            Selected Template:
            %s

            Original Resume:
            %s

            Job Description:
            %s
            """.formatted(templateName, resumeText, jobDescription);

        return callGroq(
                prompt,
                "You are a strict ATS resume builder that returns only valid JSON.",
                0.3
        );
    }

    private String callGroq(String prompt, String systemMessage, double temperature) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            String requestBody = objectMapper.writeValueAsString(
                    new GroqRequest(
                            model,
                            new Message[]{
                                    new Message("system", systemMessage),
                                    new Message("user", prompt)
                            },
                            temperature
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