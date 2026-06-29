package com.vyankatesh.resumeoptimizer.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String model;

    @Value("${nvidia.api.key:}")
    private String nvidiaApiKey;

    @Value("${nvidia.api.url:https://integrate.api.nvidia.com/v1/chat/completions}")
    private String nvidiaApiUrl;

    @Value("${nvidia.api.model:meta/llama-3.1-8b-instruct}")
    private String nvidiaModel;

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

        return callGroq(prompt, "You are an expert resume optimization assistant.", 0.4, 1800);
    }

    public String generatePromptBasedResumeEdit(
            String currentResumeText,
            String jobDescription,
            String userPrompt
    ) {
        String prompt = """
                You are an expert ATS resume editor and resume rewriting assistant.

                Your task:
                Modify the current resume according to the user's prompt and job description.

                Important rules:
                1. Do not add fake skills, tools, companies, education, certifications, achievements, or experience.
                2. If the user asks to remove a skill they do not know, remove it from every applicable section.
                3. If the user asks to remove weak lines, rewrite or remove generic lines.
                4. If the user asks to add impact metrics, add only metrics already supported by the resume or clearly label a missing metric as [add measurable result].
                5. Keep the resume ATS-friendly, professional, and suitable for the job description.
                6. Preserve every factual section from the original resume unless the user explicitly asks to remove it.
                7. Return only the final updated resume text. Do not add explanation outside the resume.

                Current Resume:
                %s

                Job Description:
                %s

                User Prompt:
                %s
                """.formatted(currentResumeText, jobDescription, userPrompt);

        return callGroq(prompt, "You are a strict, honest, ATS-friendly resume editor.", 0.2, 5000);
    }

    public String generateAiAtsAnalysis(String resumeText, String jobDescription) {
        String prompt = """
                You are an advanced ATS resume analysis engine.

                Analyze the resume against the job description for any industry.

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
                2. matchedSkills must contain only skills, tools, responsibilities, domain knowledge, qualifications, and experience found in both resume and JD.
                3. missingSkills should include important JD requirements missing from resume.
                4. Do not invent experience or certifications.
                5. Return arrays as JSON arrays of strings.
                6. Do not wrap JSON in markdown.

                Resume:
                %s

                Job Description:
                %s
                """.formatted(resumeText, jobDescription);

        return callGroq(
                prompt,
                "You are a strict ATS analysis engine that returns only valid JSON.",
                0.2,
                1800
        );
    }

    public String generateResumeBuilderContent(
            String resumeText,
            String jobDescription,
            String templateName
    ) {
        return generateResumeBuilderContent(resumeText, jobDescription, templateName, false);
    }

    public String generateResumeBuilderContent(
            String resumeText,
            String jobDescription,
            String templateName,
            boolean qualityRetry
    ) {
        String retryInstruction = qualityRetry
                ? """

                QUALITY RETRY — THE PREVIOUS DRAFT WAS NOT ACCEPTABLE:
                - The professionalSummary was too short or skills were poorly organized.
                - professionalSummary MUST contain exactly four concise factual sentences in ONE compact paragraph. Do not use newline characters.
                - Use 48 to 58 words total. Include 3 to 5 high-priority job-description keywords only when they are directly supported by the source resume.
                - Preserve every supported domain skill from the source, including terms that appear together in a comma-separated, ampersand-separated, or slash-separated skills line.
                - Return 2 to 4 meaningful, role-specific skill groups. Do not use generic labels such as Technical Skills, Soft Skills, Other Skills, or Core Skills.
                - Keep every fact truthful and return only the exact JSON schema.
                """
                : "";

        String prompt = """
                You are a professional resume writer and ATS resume builder.

                Create a complete optimized resume from the ORIGINAL RESUME and TARGET JOB DESCRIPTION.
                The result must remain factually faithful to the original resume.

                Return ONLY valid JSON. Do not add markdown, comments, or explanation outside JSON.

                Use this exact JSON schema:
                {
                  "templateName": "%s",
                  "basics": {
                    "fullName": "",
                    "headline": "",
                    "email": "",
                    "phone": "",
                    "location": "",
                    "linkedin": ""
                  },
                  "professionalSummary": "",
                  "skillGroups": [
                    { "category": "", "items": [] }
                  ],
                  "experience": [
                    {
                      "jobTitle": "",
                      "company": "",
                      "location": "",
                      "startDate": "",
                      "endDate": "",
                      "bullets": []
                    }
                  ],
                  "projects": [
                    {
                      "name": "",
                      "startDate": "",
                      "endDate": "",
                      "bullets": []
                    }
                  ],
                  "certifications": [
                    {
                      "title": "",
                      "issuer": "",
                      "year": "",
                      "details": ""
                    }
                  ],
                  "achievements": [
                    {
                      "title": "",
                      "details": ""
                    }
                  ],
                  "education": [
                    {
                      "degree": "",
                      "institution": "",
                      "location": "",
                      "startDate": "",
                      "endDate": "",
                      "details": ""
                    }
                  ]
                }

                NON-NEGOTIABLE FACTUAL RULES:
                1. Extract and retain every real section that exists in the original resume: header, summary, skills, experience, projects, certifications, achievements, and education.
                2. Do not create placeholders such as "Your Name", "Professional Headline", "Company Name", or "Project Name".
                3. Do not invent skills, technologies, certifications, employers, job titles, education, dates, metrics, awards, responsibilities, or achievements.
                4. Keep all genuine employer names, dates, education, certifications, achievements, and supported skills from the source.
                5. Use JD keywords only where they are directly supported by the original resume. Never add a JD keyword just to improve the score.
                6. Improve weak wording using clear action verbs and ATS-friendly language, but preserve the original meaning and facts.
                7. professionalSummary is REQUIRED. Write EXACTLY FOUR concise factual sentences as ONE compact paragraph with NO newline characters. Use 48 to 58 words total so the summary normally fits within four to five A4 resume lines. Include the candidate's role, exact years of experience when present, domain, major supported competencies, and factual scope of work. Naturally integrate 3 to 5 strongest TARGET JOB DESCRIPTION keywords only when each keyword is directly supported by the original resume. A summary under 48 words or above 58 words is invalid. Never add unsupported claims merely to reach the word count.
                8. Preserve every meaningful work-experience bullet from the source. Keep the same factual scope and order where practical. Rewrite for clarity only; do not collapse several responsibilities into one short bullet.
                9. For each role, retain all factual bullets supplied by the original resume. Use concise, complete bullets with an action verb and clear outcome or responsibility. Never invent metrics.
                10. SKILL EXTRACTION IS MANDATORY: inspect every source skill line. Split meaningful skills joined by commas, ampersands, slashes, or phrases such as "A, B & C" into separate items. Do not drop domain skills mentioned in the source summary, key-skills section, certifications, or experience when they are clearly supported.
                11. Return 2 to 4 concise, role-specific skillGroups when enough skills exist. Use descriptive categories derived from the source, such as "Coding Standards", "Audit, Quality & Compliance", "Systems & Documentation", "Languages & Frameworks", or "Data & Tools". Never use generic category labels such as "Technical Skills", "Soft Skills", "Other Skills", or "Core Skills". Do not put only one item in a group when other related source skills exist.
                12. PROJECT CLASSIFICATION IS MANDATORY: the experience array must contain only actual employment roles that are explicitly shown as work experience in the source. Every experience item must include a genuine employer/company taken from the source. Never use a role word such as Engineer, Developer, Analyst, Company, or Personal Project as a company value.
                13. The projects array must contain every project, product, application, platform, system, or client/personal project that appears under PROJECTS, FEATURED PROJECTS, or a PROJECT number heading in the source.
                14. Preserve every original project title exactly in projects[].name. Never replace a project title with the candidate's job title, company name, or a generic label. Never move a project into experience simply because it uses the same employer, technologies, or dates as the role.
                15. If the source contains headings such as "PROJECT 1: HOTEL BOOKING APPLICATION" or "PROJECT 2: E-COMMERCE PLATFORM", the name field must keep "Hotel Booking Application" and "E-Commerce Platform" respectively. A generic title such as "Java Full-Stack Developer" is invalid in projects[].name unless the original project heading itself uses that exact title.
                16. Before returning JSON, validate that the same project bullets do not appear in both experience[] and projects[]. A project can appear once only, inside projects[].
                17. Keep certifications, achievements, education, and project details complete whenever they exist. Do not shorten awards, credential names, issuers, years, results, or project names.
                18. Preserve content depth. If the source resume has enough content for two pages, retain enough factual detail for a complete two-page resume. Do not collapse it into a short one-page summary.
                19. Return an empty array only when that section is genuinely absent in the source. For example, do not create a Projects section when no projects exist.
                20. The selected template name must be returned exactly in templateName.
                21. Return JSON arrays for every list field, even when empty.
                22. Keep the JSON compact. Do not repeat skills, bullets, dates, company names, or section text. Do not add explanatory prose outside the schema. Keep each bullet concise and preserve no more bullets than the original source contains.
                %s

                Selected Template:
                %s

                ORIGINAL RESUME:
                %s

                TARGET JOB DESCRIPTION:
                %s
                """.formatted(templateName, retryInstruction, templateName, resumeText, jobDescription);

        return callGroq(
                prompt,
                "You are a strict factual resume builder. Return only complete valid JSON matching the requested schema.",
                0.1,
                3200
        );
    }

    /**
     * Groq remains the primary provider. NVIDIA NIM is used automatically when
     * Groq returns a rate-limit/provider error or cannot be reached.
     */
    private String callGroq(
            String prompt,
            String systemMessage,
            double temperature,
            int maxTokens
    ) {
        Exception groqFailure;

        try {
            ensureConfigured(apiKey, "Groq");
            return callProvider(
                    "Groq",
                    apiUrl,
                    apiKey,
                    model,
                    prompt,
                    systemMessage,
                    temperature,
                    maxTokens
            );
        } catch (Exception exception) {
            groqFailure = exception;
            System.out.println("[AI PROVIDER] Groq failed. Trying NVIDIA NIM fallback. Reason: "
                    + conciseMessage(exception));
        }

        if (isBlank(nvidiaApiKey)) {
            throw new RuntimeException(
                    "Groq failed and NVIDIA fallback is not configured. "
                            + "Add NVIDIA_API_KEY, then restart the backend. Groq reason: "
                            + conciseMessage(groqFailure),
                    groqFailure
            );
        }

        try {
            // NVIDIA is used only after Groq fails. Give it a larger response budget because
            // complete structured resumes can otherwise be cut off before the closing JSON arrays.
            int nvidiaMaxTokens = Math.min(Math.max(maxTokens, 3800), 4096);
            return callProvider(
                    "NVIDIA NIM",
                    nvidiaApiUrl,
                    nvidiaApiKey,
                    nvidiaModel,
                    prompt,
                    systemMessage,
                    temperature,
                    nvidiaMaxTokens
            );
        } catch (Exception nvidiaFailure) {
            throw new RuntimeException(
                    "Both AI providers failed. Groq: " + conciseMessage(groqFailure)
                            + " | NVIDIA NIM: " + conciseMessage(nvidiaFailure),
                    nvidiaFailure
            );
        }
    }

    private String callProvider(
            String providerName,
            String providerUrl,
            String providerKey,
            String providerModel,
            String prompt,
            String systemMessage,
            double temperature,
            int maxTokens
    ) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        String requestBody = objectMapper.writeValueAsString(
                new ChatRequest(
                        providerModel,
                        new Message[]{
                                new Message("system", systemMessage),
                                new Message("user", prompt)
                        },
                        temperature,
                        maxTokens
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(providerKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                providerUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new RuntimeException(providerName + " returned an empty response");
        }

        System.out.println("[AI PROVIDER] Response generated by " + providerName + ".");
        return contentNode.asText();
    }

    private void ensureConfigured(String key, String providerName) {
        if (isBlank(key)) {
            throw new IllegalStateException(providerName + " API key is not configured");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String conciseMessage(Exception exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof RestClientResponseException responseException) {
                return "HTTP " + responseException.getStatusCode().value() + ": "
                        + responseException.getResponseBodyAsString();
            }

            if (current instanceof ResourceAccessException) {
                return "Provider connection error: " + current.getMessage();
            }

            current = current.getCause();
        }

        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message;
    }

    private record ChatRequest(
            String model,
            Message[] messages,
            double temperature,
            int max_tokens
    ) {
    }

    private record Message(String role, String content) {
    }
}
