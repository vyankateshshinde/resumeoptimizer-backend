package com.vyankatesh.resumeoptimizer.jobfinder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyankatesh.resumeoptimizer.jobfinder.model.ExperienceRequirementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AiExperienceRequirementService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    AiExperienceRequirementService.class
            );

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE =
            BigDecimal.ONE;

    private static final BigDecimal MAXIMUM_REASONABLE_EXPERIENCE =
            new BigDecimal("40");

    private static final int MAXIMUM_EVIDENCE_LENGTH =
            1000;

    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate;

    private final boolean enabled;

    private final String apiKey;

    private final String apiUrl;

    private final String model;

    private final int maximumDescriptionCharacters;

    public AiExperienceRequirementService(
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder,

            @Value("${jobfinder.experience.ai-enabled:false}")
            boolean enabled,

            @Value("${groq.api.key:}")
            String apiKey,

            @Value("${groq.api.url}")
            String apiUrl,

            @Value("${groq.api.model}")
            String model,

            @Value("${jobfinder.experience.ai-max-description-characters:12000}")
            int maximumDescriptionCharacters
    ) {
        this.objectMapper =
                objectMapper;

        this.restTemplate =
                restTemplateBuilder
                        .setConnectTimeout(
                                Duration.ofSeconds(10)
                        )
                        .setReadTimeout(
                                Duration.ofSeconds(30)
                        )
                        .build();

        this.enabled =
                enabled;

        this.apiKey =
                safe(apiKey).trim();

        this.apiUrl =
                safe(apiUrl).trim();

        this.model =
                safe(model).trim();

        this.maximumDescriptionCharacters =
                Math.max(
                        1000,
                        Math.min(
                                maximumDescriptionCharacters,
                                30000
                        )
                );
    }

    /**
     * Returns an empty Optional when:
     * - AI extraction is disabled;
     * - Groq is not configured;
     * - the request fails;
     * - the response is invalid;
     * - evidence cannot be verified against the JD.
     */
    public Optional<AiExperienceRequirement> extract(
            String jobTitle,
            String jobDescription
    ) {
        if (!enabled) {
            return Optional.empty();
        }

        if (!isConfigured()) {
            log.warn(
                    "AI experience extraction is enabled, "
                            + "but Groq configuration is incomplete"
            );

            return Optional.empty();
        }

        if (!StringUtils.hasText(jobDescription)) {
            return Optional.empty();
        }

        String description =
                truncate(
                        cleanText(jobDescription),
                        maximumDescriptionCharacters
                );

        String prompt =
                buildPrompt(
                        jobTitle,
                        description
                );

        try {
            String rawResponse =
                    callGroq(prompt);

            return parseAndValidate(
                    rawResponse,
                    jobTitle,
                    description
            );

        } catch (RestClientResponseException exception) {
            log.warn(
                    "Groq experience extraction failed with HTTP {}: {}",
                    exception.getStatusCode().value(),
                    limitLogMessage(
                            exception.getResponseBodyAsString()
                    )
            );

        } catch (ResourceAccessException exception) {
            log.warn(
                    "Groq experience extraction connection failed: {}",
                    limitLogMessage(
                            exception.getMessage()
                    )
            );

        } catch (Exception exception) {
            log.warn(
                    "Groq experience extraction failed: {}",
                    limitLogMessage(
                            exception.getMessage()
                    )
            );
        }

        return Optional.empty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    private String buildPrompt(
            String jobTitle,
            String jobDescription
    ) {
        return """
                You are a strict job-description information extraction engine.

                Extract only the candidate work-experience requirement from the
                supplied job title and job description.

                Return ONLY one valid JSON object.
                Do not return markdown.
                Do not return comments.
                Do not return additional text.

                Required JSON schema:

                {
                  "minimumYears": null,
                  "maximumYears": null,
                  "requirementType": "REQUIRED",
                  "evidence": null,
                  "confidence": 0.0
                }

                Allowed requirementType values:

                REQUIRED
                PREFERRED
                NOT_SPECIFIED
                AMBIGUOUS

                Classification rules:

                1. REQUIRED:
                   The description clearly says required, mandatory, minimum,
                   must have, should have, at least, essential, qualification,
                   or otherwise states experience as an eligibility condition.

                2. PREFERRED:
                   The description clearly says preferred, desirable,
                   good to have, nice to have, plus, advantage or optional.

                3. NOT_SPECIFIED:
                   No reliable candidate work-experience requirement exists.

                4. AMBIGUOUS:
                   The description contains conflicting, unclear or multiple
                   experience requirements and the overall requirement cannot
                   be selected reliably.

                Extraction rules:

                1. Extract overall professional experience when available.
                2. Do not treat company age, product age, contract length,
                   education duration, graduation year, project duration,
                   warranty period or market presence as candidate experience.
                3. A range such as "3 to 5 years" means minimumYears=3 and
                   maximumYears=5.
                4. "5+ years" means minimumYears=5 and maximumYears=null.
                5. "No experience required" means minimumYears=0,
                   maximumYears=null and requirementType=REQUIRED.
                6. When experience is not specified, both year fields and
                   evidence must be null.
                7. evidence must be an exact short excerpt copied from the
                   supplied job description.
                8. Never invent or paraphrase evidence.
                9. confidence must be between 0 and 1.
                10. Use decimal numbers where necessary, for example 3.5.

                Job title:
                %s

                Job description:
                %s
                """.formatted(
                safe(jobTitle),
                jobDescription
        );
    }

    private String callGroq(
            String prompt
    ) throws Exception {
        ChatRequest request =
                new ChatRequest(
                        model,
                        List.of(
                                new Message(
                                        "system",
                                        "Return only valid JSON. "
                                                + "Never invent experience evidence."
                                ),
                                new Message(
                                        "user",
                                        prompt
                                )
                        ),
                        0.0,
                        400
                );

        String requestBody =
                objectMapper.writeValueAsString(
                        request
                );

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setBearerAuth(
                apiKey
        );

        HttpEntity<String> entity =
                new HttpEntity<>(
                        requestBody,
                        headers
                );

        String responseBody =
                restTemplate.postForObject(
                        apiUrl,
                        entity,
                        String.class
                );

        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalStateException(
                    "Groq returned an empty response"
            );
        }

        JsonNode root =
                objectMapper.readTree(
                        responseBody
                );

        JsonNode contentNode =
                root.path("choices")
                        .path(0)
                        .path("message")
                        .path("content");

        if (contentNode.isMissingNode()
                || !StringUtils.hasText(
                contentNode.asText()
        )) {
            throw new IllegalStateException(
                    "Groq response did not contain message content"
            );
        }

        return contentNode.asText();
    }

    private Optional<AiExperienceRequirement>
    parseAndValidate(
            String rawResponse,
            String jobTitle,
            String jobDescription
    ) {
        try {
            String json =
                    extractJsonObject(
                            rawResponse
                    );

            JsonNode root =
                    objectMapper.readTree(json);

            BigDecimal minimumYears =
                    readNullableDecimal(
                            root,
                            "minimumYears"
                    );

            BigDecimal maximumYears =
                    readNullableDecimal(
                            root,
                            "maximumYears"
                    );

            ExperienceRequirementType requirementType =
                    readRequirementType(
                            root.path(
                                    "requirementType"
                            ).asText(null)
                    );

            String evidence =
                    readNullableText(
                            root,
                            "evidence"
                    );

            BigDecimal confidence =
                    normalizeConfidence(
                            readNullableDecimal(
                                    root,
                                    "confidence"
                            )
                    );

            if (requirementType == null) {
                return Optional.empty();
            }

            if (!validExperienceValues(
                    minimumYears,
                    maximumYears
            )) {
                return Optional.empty();
            }

            if (requirementType
                    == ExperienceRequirementType.NOT_SPECIFIED) {

                if (minimumYears != null
                        || maximumYears != null) {
                    return Optional.empty();
                }

                return Optional.of(
                        new AiExperienceRequirement(
                                null,
                                null,
                                ExperienceRequirementType
                                        .NOT_SPECIFIED,
                                null,
                                confidence,
                                "AI"
                        )
                );
            }

            if (
                    (
                            requirementType
                                    == ExperienceRequirementType.REQUIRED
                                    || requirementType
                                    == ExperienceRequirementType.PREFERRED
                    )
                            && minimumYears == null
            ) {
                return Optional.empty();
            }

            if (!StringUtils.hasText(evidence)) {
                return Optional.empty();
            }

            if (!evidenceExistsInSource(
                    evidence,
                    jobTitle,
                    jobDescription
            )) {
                log.warn(
                        "AI experience evidence was rejected because "
                                + "it was not found in the job description"
                );

                return Optional.empty();
            }

            return Optional.of(
                    new AiExperienceRequirement(
                            minimumYears,
                            maximumYears,
                            requirementType,
                            truncate(
                                    evidence.trim(),
                                    MAXIMUM_EVIDENCE_LENGTH
                            ),
                            confidence,
                            "AI"
                    )
            );

        } catch (Exception exception) {
            log.warn(
                    "Could not parse AI experience response: {}",
                    limitLogMessage(
                            exception.getMessage()
                    )
            );

            return Optional.empty();
        }
    }

    private BigDecimal readNullableDecimal(
            JsonNode root,
            String fieldName
    ) {
        JsonNode node =
                root.path(fieldName);

        if (node.isMissingNode()
                || node.isNull()) {
            return null;
        }

        if (node.isNumber()) {
            return node.decimalValue();
        }

        String value =
                node.asText();

        if (!StringUtils.hasText(value)
                || "null".equalsIgnoreCase(
                value.trim()
        )) {
            return null;
        }

        return new BigDecimal(
                value.trim()
        );
    }

    private String readNullableText(
            JsonNode root,
            String fieldName
    ) {
        JsonNode node =
                root.path(fieldName);

        if (node.isMissingNode()
                || node.isNull()) {
            return null;
        }

        String value =
                node.asText();

        return StringUtils.hasText(value)
                ? value.trim()
                : null;
    }

    private ExperienceRequirementType readRequirementType(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return ExperienceRequirementType.valueOf(
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean validExperienceValues(
            BigDecimal minimumYears,
            BigDecimal maximumYears
    ) {
        if (minimumYears != null
                && !isReasonableExperience(
                minimumYears
        )) {
            return false;
        }

        if (maximumYears != null
                && !isReasonableExperience(
                maximumYears
        )) {
            return false;
        }

        return minimumYears == null
                || maximumYears == null
                || minimumYears.compareTo(
                maximumYears
        ) <= 0;
    }

    private boolean isReasonableExperience(
            BigDecimal value
    ) {
        return value.compareTo(ZERO) >= 0
                && value.compareTo(
                MAXIMUM_REASONABLE_EXPERIENCE
        ) <= 0;
    }

    private BigDecimal normalizeConfidence(
            BigDecimal confidence
    ) {
        if (confidence == null) {
            return new BigDecimal("0.50");
        }

        if (confidence.compareTo(ZERO) < 0) {
            return ZERO;
        }

        if (confidence.compareTo(ONE) > 0) {
            return ONE;
        }

        return confidence;
    }

    private boolean evidenceExistsInSource(
            String evidence,
            String jobTitle,
            String jobDescription
    ) {
        String normalizedEvidence =
                normalizeForEvidenceComparison(
                        evidence
                );

        String normalizedDescription =
                normalizeForEvidenceComparison(
                        jobDescription
                );

        if (normalizedDescription.contains(
                normalizedEvidence
        )) {
            return true;
        }

        String normalizedCompleteText =
                normalizeForEvidenceComparison(
                        safe(jobTitle)
                                + " "
                                + safe(jobDescription)
                );

        return normalizedCompleteText.contains(
                normalizedEvidence
        );
    }

    private String normalizeForEvidenceComparison(
            String value
    ) {
        return cleanText(value)
                .toLowerCase(Locale.ROOT)
                .replace('–', '-')
                .replace('—', '-')
                .replaceAll(
                        "[“”\"'`]",
                        ""
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private String extractJsonObject(
            String rawResponse
    ) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new IllegalArgumentException(
                    "AI response is empty"
            );
        }

        int firstBrace =
                rawResponse.indexOf('{');

        int lastBrace =
                rawResponse.lastIndexOf('}');

        if (firstBrace < 0
                || lastBrace < firstBrace) {
            throw new IllegalArgumentException(
                    "AI response does not contain a JSON object"
            );
        }

        return rawResponse.substring(
                firstBrace,
                lastBrace + 1
        );
    }

    private boolean isConfigured() {
        return StringUtils.hasText(apiKey)
                && StringUtils.hasText(apiUrl)
                && StringUtils.hasText(model);
    }

    private String cleanText(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value
                .replaceAll(
                        "(?i)<br\\s*/?>",
                        "\n"
                )
                .replaceAll(
                        "(?i)</p>|(?i)</li>|(?i)</div>",
                        "\n"
                )
                .replaceAll(
                        "<[^>]+>",
                        " "
                )
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll(
                        "[\\t\\x0B\\f\\r ]+",
                        " "
                )
                .replaceAll(
                        "\\n\\s+",
                        "\n"
                )
                .replaceAll(
                        "\\n{3,}",
                        "\n\n"
                )
                .trim();
    }

    private String truncate(
            String value,
            int maximumLength
    ) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maximumLength) {
            return value;
        }

        return value.substring(
                0,
                maximumLength
        );
    }

    private String limitLogMessage(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return "unknown error";
        }

        String normalized =
                value.replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        return truncate(
                normalized,
                500
        );
    }

    private static String safe(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }

    public record AiExperienceRequirement(
            BigDecimal minimumYears,
            BigDecimal maximumYears,
            ExperienceRequirementType requirementType,
            String evidence,
            BigDecimal confidence,
            String extractionMethod
    ) {

        public ExperienceRequirementExtractorService
                .ExperienceRequirement toExperienceRequirement() {

            return new ExperienceRequirementExtractorService
                    .ExperienceRequirement(
                    minimumYears,
                    maximumYears,
                    requirementType,
                    evidence,
                    confidence,
                    extractionMethod
            );
        }
    }

    private record ChatRequest(
            String model,
            List<Message> messages,
            double temperature,
            int max_tokens
    ) {
    }

    private record Message(
            String role,
            String content
    ) {
    }
}