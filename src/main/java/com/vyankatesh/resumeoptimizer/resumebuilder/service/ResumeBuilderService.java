package com.vyankatesh.resumeoptimizer.resumebuilder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vyankatesh.resumeoptimizer.ai.service.GroqService;
import com.vyankatesh.resumeoptimizer.resume.ResumeEntity;
import com.vyankatesh.resumeoptimizer.resume.ResumeRepository;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.AchievementItem;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.CertificationItem;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.EducationItem;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ExperienceItem;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ProjectItem;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBasics;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderHistoryResponse;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderRequest;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderRefineRequest;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.ResumeBuilderResponse;
import com.vyankatesh.resumeoptimizer.resumebuilder.dto.SkillGroup;
import com.vyankatesh.resumeoptimizer.resumebuilder.entity.ResumeBuilderHistory;
import com.vyankatesh.resumeoptimizer.resumebuilder.repository.ResumeBuilderHistoryRepository;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionRequest;
import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionResponse;
import com.vyankatesh.resumeoptimizer.resumeversion.service.ResumeVersionService;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
public class ResumeBuilderService {

    private final ResumeRepository resumeRepository;
    private final GroqService groqService;
    private final ObjectMapper objectMapper;
    private final ResumeBuilderHistoryRepository historyRepository;
    private final ResumeVersionService resumeVersionService;

    public ResumeBuilderService(
            ResumeRepository resumeRepository,
            GroqService groqService,
            ObjectMapper objectMapper,
            ResumeBuilderHistoryRepository historyRepository,
            ResumeVersionService resumeVersionService
    ) {
        this.resumeRepository = resumeRepository;
        this.groqService = groqService;
        this.objectMapper = objectMapper;
        this.historyRepository = historyRepository;
        this.resumeVersionService = resumeVersionService;
    }

    public ResumeBuilderResponse generateResume(
            ResumeBuilderRequest request,
            String userEmail
    ) {
        validateGenerateRequest(request);

        String templateName = normalizeText(request.getTemplateName());
        if (templateName.isBlank()) {
            templateName = "ATS Professional";
        }

        ResumeEntity resume = getOwnedResume(request.getResumeId(), userEmail);

        String resumeText = normalizeSourceResumeText(resume.getExtractedText());
        if (resumeText.isBlank()) {
            throw new RuntimeException(
                    "Resume extracted text is empty. Please upload the resume again."
            );
        }

        try {
            String aiJson = groqService.generateResumeBuilderContent(
                    resumeText,
                    request.getJobDescription().trim(),
                    templateName
            );

            ResumeBuilderResponse response = parseAndNormalizeResume(
                    aiJson,
                    templateName,
                    resumeText
            );

            if (requiresContentQualityRetry(response)) {
                String retryJson = groqService.generateResumeBuilderContent(
                        resumeText,
                        request.getJobDescription().trim(),
                        templateName,
                        true
                );

                response = parseAndNormalizeResume(
                        retryJson,
                        templateName,
                        resumeText
                );
            }

            saveBuilderHistory(
                    request.getResumeId(),
                    userEmail,
                    request.getJobDescription(),
                    response
            );

            return response;

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to generate structured resume content: " + exception.getMessage()
            );
        }
    }


    public ResumeBuilderResponse refineResume(
            ResumeBuilderRefineRequest request,
            String userEmail
    ) {
        validateRefineRequest(request);

        ResumeEntity resume = getOwnedResume(request.getResumeId(), userEmail);

        String templateName = normalizeText(request.getTemplateName());
        if (templateName.isBlank()) {
            templateName = normalizeText(request.getResume().getTemplateName());
        }
        if (templateName.isBlank()) {
            templateName = "ATS Professional";
        }

        ResumeBuilderResponse currentResume = request.getResume();
        normalizeStructuredResponse(currentResume, templateName);

        String currentResumeText = buildFullResumeText(currentResume);
        if (currentResumeText.isBlank()) {
            throw new RuntimeException("Current resume content is required for AI refinement");
        }

        String jobDescription = normalizeText(request.getJobDescription());
        String safePrompt = normalizeText(request.getUserPrompt());

        String refinementInstruction = """
                \n\nUSER REFINEMENT REQUEST:
                %s

                IMPORTANT RULES:
                - Apply the user request to the resume content.
                - Preserve genuine names, employers, dates, degrees and technologies from the current resume.
                - Preserve section classification: do not move a project into Professional Experience and do not replace a project name with a job title or company name.
                - Do not invent numbers, percentages, users, revenue, rankings, certifications or employers.
                - When the user asks for metrics that are not supported by the resume, use a clear placeholder such as [add measurable result] instead of inventing a metric.
                - Return only the same valid structured JSON resume format.
                """.formatted(safePrompt);

        try {
            String aiJson = groqService.generateResumeBuilderContent(
                    currentResumeText,
                    jobDescription + refinementInstruction,
                    templateName
            );

            JsonNode rootNode = readResumeJson(aiJson);
            normalizeAiJsonBeforeMapping(rootNode);

            ResumeBuilderResponse refinedResume = objectMapper.treeToValue(
                    rootNode,
                    ResumeBuilderResponse.class
            );

            normalizeStructuredResponse(refinedResume, templateName);
            repairProjectClassificationFromSource(refinedResume, resume.getExtractedText());
            normalizeStructuredResponse(refinedResume, templateName);
            saveBuilderHistory(
                    request.getResumeId(),
                    userEmail,
                    request.getJobDescription(),
                    refinedResume
            );

            return refinedResume;
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to refine structured resume content: " + exception.getMessage()
            );
        }
    }

    public List<ResumeBuilderHistoryResponse> getHistory(String userEmail) {
        return historyRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::mapHistoryToResponse)
                .collect(Collectors.toList());
    }

    public ResumeVersionResponse saveHistoryAsVersion(Long historyId, String userEmail) {
        ResumeBuilderHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException(
                        "Resume builder history not found with id: " + historyId
                ));

        if (!history.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("You are not allowed to save this resume history");
        }

        try {
            ResumeBuilderResponse builderResponse = objectMapper.readValue(
                    history.getGeneratedResumeJson(),
                    ResumeBuilderResponse.class
            );

            normalizeStructuredResponse(
                    builderResponse,
                    normalizeText(history.getTemplateName()).isBlank()
                            ? "ATS Professional"
                            : history.getTemplateName()
            );

            ResumeVersionRequest versionRequest = new ResumeVersionRequest();
            versionRequest.setResumeId(history.getResumeId());
            versionRequest.setVersionName("AI Generated Resume - " + builderResponse.getTemplateName());
            versionRequest.setTemplateName(builderResponse.getTemplateName());
            versionRequest.setFullResumeText(builderResponse.getFullResumeText());
            versionRequest.setProfessionalSummary(builderResponse.getProfessionalSummary());
            versionRequest.setSkills(joinList(builderResponse.getSkills()));
            versionRequest.setExperienceBullets(joinList(builderResponse.getExperienceBullets()));
            versionRequest.setProjectBullets(joinList(builderResponse.getProjectBullets()));
            versionRequest.setEducation(builderResponse.getEducationText());
            versionRequest.setJobDescription(history.getJobDescription());

            return resumeVersionService.saveVersion(versionRequest, userEmail);

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to save builder history as resume version: " + exception.getMessage()
            );
        }
    }

    private ResumeBuilderHistoryResponse mapHistoryToResponse(ResumeBuilderHistory history) {
        try {
            ResumeBuilderResponse response = objectMapper.readValue(
                    history.getGeneratedResumeJson(),
                    ResumeBuilderResponse.class
            );

            normalizeStructuredResponse(
                    response,
                    normalizeText(history.getTemplateName()).isBlank()
                            ? "ATS Professional"
                            : history.getTemplateName()
            );

            return new ResumeBuilderHistoryResponse(
                    history.getId(),
                    history.getResumeId(),
                    history.getTemplateName(),
                    history.getJobDescription(),
                    history.getCreatedAt(),
                    response
            );
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to read resume builder history with id: " + history.getId()
            );
        }
    }

    private ResumeBuilderResponse parseAndNormalizeResume(
            String aiJson,
            String templateName,
            String sourceResumeText
    ) throws Exception {
        JsonNode rootNode = readResumeJson(aiJson);
        normalizeAiJsonBeforeMapping(rootNode);

        ResumeBuilderResponse response = objectMapper.treeToValue(
                rootNode,
                ResumeBuilderResponse.class
        );

        hydrateMissingContactFromSource(response, sourceResumeText);
        normalizeStructuredResponse(response, templateName);
        repairProjectClassificationFromSource(response, sourceResumeText);
        normalizeStructuredResponse(response, templateName);
        return response;
    }

    private boolean requiresContentQualityRetry(ResumeBuilderResponse response) {
        if (response == null) {
            return true;
        }

        // Do not make a second full AI call only because a summary has a different word count
        // or a skill category is generic. A second generation wastes tokens and can return a
        // truncated JSON response. Retry only when the first response has no usable resume data.
        boolean hasSummary = !normalizeText(response.getProfessionalSummary()).isBlank();
        boolean hasExperience = response.getExperience() != null && !response.getExperience().isEmpty();
        boolean hasProjects = response.getProjects() != null && !response.getProjects().isEmpty();
        boolean hasEducation = response.getEducation() != null && !response.getEducation().isEmpty();

        return !hasSummary && !hasExperience && !hasProjects && !hasEducation;
    }

    private int countWords(String value) {
        String cleanValue = normalizeText(value);

        if (cleanValue.isBlank()) {
            return 0;
        }

        return cleanValue.split("\\s+").length;
    }

    private int countSummarySentences(String value) {
        String cleanValue = normalizeText(value);

        if (cleanValue.isBlank()) {
            return 0;
        }

        String[] sentences = cleanValue.split("(?<=[.!?])\\s+");
        int count = 0;

        for (String sentence : sentences) {
            if (!normalizeText(sentence).isBlank()) {
                count++;
            }
        }

        return count;
    }

    private boolean hasGenericSkillCategories(List<SkillGroup> skillGroups) {
        if (skillGroups == null || skillGroups.isEmpty()) {
            return false;
        }

        for (SkillGroup group : skillGroups) {
            String category = normalizeText(group == null ? "" : group.getCategory())
                    .toLowerCase();

            if (category.equals("technical skill")
                    || category.equals("technical skills")
                    || category.equals("soft skill")
                    || category.equals("soft skills")
                    || category.equals("other skill")
                    || category.equals("other skills")
                    || category.equals("core skill")
                    || category.equals("core skills")
                    || category.equals("skills")) {
                return true;
            }
        }

        return false;
    }

    private ResumeEntity getOwnedResume(Long resumeId, String userEmail) {
        ResumeEntity resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException(
                        "Resume not found with id: " + resumeId
                ));

        String resumeEmail = normalizeText(resume.getEmail()).toLowerCase();
        String loggedInEmail = normalizeText(userEmail).toLowerCase();

        if (!resumeEmail.equals(loggedInEmail)) {
            throw new RuntimeException("You are not allowed to use this resume");
        }

        return resume;
    }

    private void saveBuilderHistory(
            Long resumeId,
            String userEmail,
            String jobDescription,
            ResumeBuilderResponse response
    ) throws Exception {
        String normalizedJson = objectMapper.writeValueAsString(response);

        ResumeBuilderHistory history = new ResumeBuilderHistory();
        history.setResumeId(resumeId);
        history.setUserEmail(normalizeText(userEmail));
        history.setTemplateName(response.getTemplateName());
        history.setJobDescription(normalizeText(jobDescription));
        history.setGeneratedResumeJson(normalizedJson);

        historyRepository.save(history);
    }

    private void validateRefineRequest(ResumeBuilderRefineRequest request) {
        if (request == null) {
            throw new RuntimeException("Resume refinement request is required");
        }

        if (request.getResumeId() == null) {
            throw new RuntimeException("Resume ID is required");
        }

        if (request.getResume() == null) {
            throw new RuntimeException("Current resume is required");
        }

        if (normalizeText(request.getUserPrompt()).isBlank()) {
            throw new RuntimeException("AI refinement prompt is required");
        }
    }

    private String cleanAiJson(String aiJson) {
        String cleanJson = normalizeText(aiJson);

        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7).trim();
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3).trim();
        }

        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3).trim();
        }

        int firstBrace = cleanJson.indexOf("{");
        int lastBrace = cleanJson.lastIndexOf("}");

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return cleanJson.substring(firstBrace, lastBrace + 1);
        }

        return cleanJson;
    }

    private JsonNode readResumeJson(String aiJson) throws Exception {
        String cleanJson = cleanAiJson(aiJson);

        try {
            return objectMapper.readTree(cleanJson);
        } catch (Exception originalException) {
            String repairedJson = repairTruncatedJson(cleanJson);

            try {
                JsonNode repairedNode = objectMapper.readTree(repairedJson);
                System.out.println("[AI JSON] Repaired an incomplete provider response and preserved the completed resume content.");
                return repairedNode;
            } catch (Exception repairException) {
                throw new RuntimeException(
                        "AI returned incomplete JSON. Please generate the resume once again.",
                        originalException
                );
            }
        }
    }

    private String repairTruncatedJson(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }

        int firstObject = value.indexOf('{');
        String source = firstObject >= 0 ? value.substring(firstObject).trim() : value.trim();
        StringBuilder repaired = new StringBuilder(source);
        Deque<Character> expectedClosers = new ArrayDeque<>();
        boolean insideString = false;
        boolean escaped = false;

        for (int index = 0; index < repaired.length(); index++) {
            char current = repaired.charAt(index);

            if (insideString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    insideString = false;
                }
                continue;
            }

            if (current == '"') {
                insideString = true;
            } else if (current == '{') {
                expectedClosers.push('}');
            } else if (current == '[') {
                expectedClosers.push(']');
            } else if ((current == '}' || current == ']')
                    && !expectedClosers.isEmpty()
                    && expectedClosers.peek() == current) {
                expectedClosers.pop();
            }
        }

        if (insideString) {
            repaired.append('"');
        }

        while (repaired.length() > 0 && Character.isWhitespace(repaired.charAt(repaired.length() - 1))) {
            repaired.deleteCharAt(repaired.length() - 1);
        }

        if (repaired.length() > 0 && repaired.charAt(repaired.length() - 1) == ',') {
            repaired.deleteCharAt(repaired.length() - 1);
        }

        if (repaired.length() > 0 && repaired.charAt(repaired.length() - 1) == ':') {
            repaired.append("null");
        }

        while (!expectedClosers.isEmpty()) {
            repaired.append(expectedClosers.pop());
        }

        return repaired.toString();
    }

    private void validateGenerateRequest(ResumeBuilderRequest request) {
        if (request == null) {
            throw new RuntimeException("Resume builder request is required");
        }

        if (request.getResumeId() == null) {
            throw new RuntimeException("Resume ID is required");
        }

        if (request.getJobDescription() == null || request.getJobDescription().isBlank()) {
            throw new RuntimeException("Job description is required");
        }
    }

    private void normalizeAiJsonBeforeMapping(JsonNode rootNode) {
        if (rootNode == null || !rootNode.isObject()) {
            return;
        }

        ObjectNode objectNode = (ObjectNode) rootNode;

        normalizeResponseAliases(objectNode);

        normalizeTextFieldToArrayOfObjects(
                objectNode,
                "education",
                "degree",
                new String[]{"institution", "location", "startDate", "endDate", "details"}
        );

        normalizeTextFieldToArrayOfObjects(
                objectNode,
                "experience",
                "jobTitle",
                new String[]{"company", "location", "startDate", "endDate", "bullets"}
        );

        normalizeTextFieldToArrayOfObjects(
                objectNode,
                "projects",
                "name",
                new String[]{"startDate", "endDate", "bullets"}
        );

        normalizeTextFieldToArrayOfObjects(
                objectNode,
                "certifications",
                "title",
                new String[]{"issuer", "year", "details"}
        );

        normalizeTextFieldToArrayOfObjects(
                objectNode,
                "achievements",
                "title",
                new String[]{"details"}
        );

        normalizeSkillGroups(objectNode);
        normalizeNestedBulletFields(objectNode, "experience");
        normalizeNestedBulletFields(objectNode, "projects");
    }

    private void normalizeNestedBulletFields(ObjectNode objectNode, String fieldName) {
        JsonNode sectionNode = objectNode.get(fieldName);

        if (sectionNode == null || !sectionNode.isArray()) {
            return;
        }

        for (JsonNode itemNode : sectionNode) {
            if (itemNode instanceof ObjectNode objectItem
                    && objectItem.has("bullets")
                    && objectItem.get("bullets").isTextual()) {
                ArrayNode bulletsArray = objectMapper.createArrayNode();

                for (String bullet : objectItem.get("bullets").asText().split("\\n|•|-")) {
                    String cleanBullet = normalizeText(bullet);

                    if (!cleanBullet.isBlank()) {
                        bulletsArray.add(cleanBullet);
                    }
                }

                objectItem.set("bullets", bulletsArray);
            }
        }
    }

    private void normalizeResponseAliases(ObjectNode objectNode) {
        if (!objectNode.has("professionalSummary") && objectNode.has("summary")) {
            objectNode.set("professionalSummary", objectNode.get("summary"));
        }

        if (!objectNode.has("skillGroups") && objectNode.has("skills")) {
            JsonNode skillsNode = objectNode.get("skills");
            ArrayNode skillGroupsArray = objectMapper.createArrayNode();
            ObjectNode groupNode = objectMapper.createObjectNode();
            groupNode.put("category", "Technical Skills");

            ArrayNode itemsArray = objectMapper.createArrayNode();
            if (skillsNode.isArray()) {
                for (JsonNode skill : skillsNode) {
                    if (skill.isTextual() && !normalizeText(skill.asText()).isBlank()) {
                        itemsArray.add(normalizeText(skill.asText()));
                    }
                }
            } else if (skillsNode.isTextual()) {
                for (String skill : skillsNode.asText().split(",")) {
                    if (!normalizeText(skill).isBlank()) {
                        itemsArray.add(normalizeText(skill));
                    }
                }
            }

            groupNode.set("items", itemsArray);
            skillGroupsArray.add(groupNode);
            objectNode.set("skillGroups", skillGroupsArray);
        }

        if (!objectNode.has("certifications") && objectNode.has("certificates")) {
            objectNode.set("certifications", objectNode.get("certificates"));
        }

        if (!objectNode.has("achievements") && objectNode.has("awards")) {
            objectNode.set("achievements", objectNode.get("awards"));
        }

        if ((!objectNode.has("basics") || !objectNode.get("basics").isObject())
                && (objectNode.has("fullName") || objectNode.has("email") || objectNode.has("phone"))) {
            ObjectNode basicsNode = objectMapper.createObjectNode();
            copyRootFieldToBasics(objectNode, basicsNode, "fullName");
            copyRootFieldToBasics(objectNode, basicsNode, "headline");
            copyRootFieldToBasics(objectNode, basicsNode, "email");
            copyRootFieldToBasics(objectNode, basicsNode, "phone");
            copyRootFieldToBasics(objectNode, basicsNode, "location");
            copyRootFieldToBasics(objectNode, basicsNode, "linkedin");
            objectNode.set("basics", basicsNode);
        }
    }

    private void copyRootFieldToBasics(ObjectNode source, ObjectNode target, String fieldName) {
        if (source.has(fieldName) && source.get(fieldName).isTextual()) {
            target.put(fieldName, normalizeText(source.get(fieldName).asText()));
        }
    }

    private void normalizeTextFieldToArrayOfObjects(
            ObjectNode objectNode,
            String fieldName,
            String mainFieldName,
            String[] additionalFields
    ) {
        if (!objectNode.has(fieldName) || objectNode.get(fieldName).isNull()) {
            return;
        }

        JsonNode fieldNode = objectNode.get(fieldName);

        ArrayNode arrayNode = objectMapper.createArrayNode();

        if (fieldNode.isArray()) {
            for (JsonNode itemNode : fieldNode) {
                if (itemNode.isObject()) {
                    arrayNode.add(itemNode);
                    continue;
                }

                if (itemNode.isTextual() && !normalizeText(itemNode.asText()).isBlank()) {
                    arrayNode.add(createObjectFromText(
                            mainFieldName,
                            normalizeText(itemNode.asText()),
                            additionalFields
                    ));
                }
            }

            objectNode.set(fieldName, arrayNode);
            return;
        }

        if (fieldNode.isObject()) {
            arrayNode.add(fieldNode);
            objectNode.set(fieldName, arrayNode);
            return;
        }

        if (fieldNode.isTextual()) {
            String value = normalizeText(fieldNode.asText());

            if (!value.isBlank()) {
                arrayNode.add(createObjectFromText(mainFieldName, value, additionalFields));
            }

            objectNode.set(fieldName, arrayNode);
        }
    }

    private ObjectNode createObjectFromText(
            String mainFieldName,
            String value,
            String[] additionalFields
    ) {
        ObjectNode itemNode = objectMapper.createObjectNode();
        itemNode.put(mainFieldName, value);

        for (String additionalField : additionalFields) {
            if ("bullets".equals(additionalField)) {
                itemNode.set(additionalField, objectMapper.createArrayNode());
            } else {
                itemNode.put(additionalField, "");
            }
        }

        return itemNode;
    }

    private void normalizeSkillGroups(ObjectNode objectNode) {
        if (!objectNode.has("skillGroups") || objectNode.get("skillGroups").isNull()) {
            return;
        }

        JsonNode skillGroupsNode = objectNode.get("skillGroups");

        if (skillGroupsNode.isArray()) {
            boolean containsTextOnly = true;

            for (JsonNode itemNode : skillGroupsNode) {
                if (!itemNode.isTextual()) {
                    containsTextOnly = false;
                    break;
                }
            }

            if (!containsTextOnly) {
                return;
            }

            ArrayNode itemsArray = objectMapper.createArrayNode();
            for (JsonNode itemNode : skillGroupsNode) {
                String skill = normalizeText(itemNode.asText());
                if (!skill.isBlank()) {
                    itemsArray.add(skill);
                }
            }

            ArrayNode normalizedGroups = objectMapper.createArrayNode();
            ObjectNode groupNode = objectMapper.createObjectNode();
            groupNode.put("category", "Technical Skills");
            groupNode.set("items", itemsArray);
            normalizedGroups.add(groupNode);
            objectNode.set("skillGroups", normalizedGroups);
            return;
        }

        ArrayNode skillGroupsArray = objectMapper.createArrayNode();

        if (skillGroupsNode.isObject()) {
            skillGroupsArray.add(skillGroupsNode);
            objectNode.set("skillGroups", skillGroupsArray);
            return;
        }

        if (skillGroupsNode.isTextual()) {
            String skillsText = normalizeText(skillGroupsNode.asText());

            if (!skillsText.isBlank()) {
                ObjectNode groupNode = objectMapper.createObjectNode();
                groupNode.put("category", "Technical Skills");

                ArrayNode itemsArray = objectMapper.createArrayNode();

                for (String skill : skillsText.split(",")) {
                    String cleanSkill = normalizeText(skill);

                    if (!cleanSkill.isBlank()) {
                        itemsArray.add(cleanSkill);
                    }
                }

                groupNode.set("items", itemsArray);
                skillGroupsArray.add(groupNode);
            }

            objectNode.set("skillGroups", skillGroupsArray);
        }
    }

    private void normalizeStructuredResponse(
            ResumeBuilderResponse response,
            String selectedTemplateName
    ) {
        if (response == null) {
            throw new RuntimeException("AI returned an empty resume response");
        }

        response.setTemplateName(selectedTemplateName);

        if (response.getBasics() == null) {
            response.setBasics(new ResumeBasics());
        }

        normalizeBasics(response.getBasics());

        response.setProfessionalSummary(normalizeText(response.getProfessionalSummary()));
        response.setSkillGroups(normalizeSkillGroups(response.getSkillGroups()));
        response.setExperience(normalizeExperience(response.getExperience()));
        response.setProjects(normalizeProjects(response.getProjects()));
        response.setCertifications(normalizeCertifications(response.getCertifications()));
        response.setAchievements(normalizeAchievements(response.getAchievements()));
        response.setEducation(normalizeEducation(response.getEducation()));

        if (
                response.getProfessionalSummary().isBlank()
                        && response.getExperience().isEmpty()
                        && response.getProjects().isEmpty()
                        && response.getCertifications().isEmpty()
                        && response.getAchievements().isEmpty()
                        && response.getEducation().isEmpty()
        ) {
            throw new RuntimeException(
                    "AI returned too little content. Please generate the resume again."
            );
        }

        response.setSkills(flattenSkills(response.getSkillGroups()));
        response.setExperienceBullets(flattenExperienceBullets(response.getExperience()));
        response.setProjectBullets(flattenProjectBullets(response.getProjects()));
        response.setEducationText(buildEducationText(response.getEducation()));
        response.setFullResumeText(buildFullResumeText(response));
    }

    private void hydrateMissingContactFromSource(
            ResumeBuilderResponse response,
            String sourceResumeText
    ) {
        if (response == null || response.getBasics() == null) {
            return;
        }

        ResumeBasics basics = response.getBasics();
        String source = sourceResumeText == null ? "" : sourceResumeText;

        if (normalizeText(basics.getEmail()).isBlank()) {
            java.util.regex.Matcher emailMatcher = java.util.regex.Pattern
                    .compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}")
                    .matcher(source);

            if (emailMatcher.find()) {
                basics.setEmail(emailMatcher.group());
            }
        }

        if (normalizeText(basics.getPhone()).isBlank()) {
            java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern
                    .compile("(?<!\\d)(?:\\+?\\d[\\d\\s-]{7,}\\d)(?!\\d)")
                    .matcher(source);

            if (phoneMatcher.find()) {
                basics.setPhone(phoneMatcher.group().trim());
            }
        }
    }

    private void normalizeBasics(ResumeBasics basics) {
        basics.setFullName(normalizeText(basics.getFullName()));
        basics.setHeadline(normalizeText(basics.getHeadline()));
        basics.setEmail(normalizeText(basics.getEmail()));
        basics.setPhone(normalizeText(basics.getPhone()));
        basics.setLocation(normalizeText(basics.getLocation()));
        basics.setLinkedin(normalizeText(basics.getLinkedin()));
    }

    private List<SkillGroup> normalizeSkillGroups(List<SkillGroup> skillGroups) {
        if (skillGroups == null) {
            return new ArrayList<>();
        }

        List<SkillGroup> normalized = new ArrayList<>();

        for (SkillGroup group : skillGroups) {
            if (group == null) {
                continue;
            }

            String category = normalizeText(group.getCategory());
            List<String> items = normalizeStringList(group.getItems());

            if (!category.isBlank() && !items.isEmpty()) {
                SkillGroup cleanGroup = new SkillGroup();
                cleanGroup.setCategory(category);
                cleanGroup.setItems(items);
                normalized.add(cleanGroup);
            }
        }

        return normalized;
    }

    private List<ExperienceItem> normalizeExperience(List<ExperienceItem> experience) {
        if (experience == null) {
            return new ArrayList<>();
        }

        List<ExperienceItem> normalized = new ArrayList<>();

        for (ExperienceItem item : experience) {
            if (item == null) {
                continue;
            }

            ExperienceItem cleanItem = new ExperienceItem();
            cleanItem.setJobTitle(normalizeText(item.getJobTitle()));
            cleanItem.setCompany(normalizeText(item.getCompany()));
            cleanItem.setLocation(normalizeText(item.getLocation()));
            cleanItem.setStartDate(normalizeText(item.getStartDate()));
            cleanItem.setEndDate(normalizeText(item.getEndDate()));
            cleanItem.setBullets(normalizeStringList(item.getBullets()));

            if (
                    !cleanItem.getJobTitle().isBlank()
                            || !cleanItem.getCompany().isBlank()
                            || !cleanItem.getBullets().isEmpty()
            ) {
                normalized.add(cleanItem);
            }
        }

        return normalized;
    }

    private List<ProjectItem> normalizeProjects(List<ProjectItem> projects) {
        if (projects == null) {
            return new ArrayList<>();
        }

        List<ProjectItem> normalized = new ArrayList<>();

        for (ProjectItem item : projects) {
            if (item == null) {
                continue;
            }

            ProjectItem cleanItem = new ProjectItem();
            cleanItem.setName(normalizeText(item.getName()));
            cleanItem.setStartDate(normalizeText(item.getStartDate()));
            cleanItem.setEndDate(normalizeText(item.getEndDate()));
            cleanItem.setBullets(normalizeStringList(item.getBullets()));

            if (!cleanItem.getName().isBlank() || !cleanItem.getBullets().isEmpty()) {
                normalized.add(cleanItem);
            }
        }

        return normalized;
    }

    /**
     * Protects genuine project titles when an LLM incorrectly categorizes a project as
     * a repeated employment entry. The original parsed resume remains the source of truth.
     */
    /**
     * Uses the original parsed resume as the source of truth for section ownership.
     * A project must never remain as a duplicate employment role when the generated
     * Projects section already contains the same bullet content.
     */
    private void repairProjectClassificationFromSource(
            ResumeBuilderResponse response,
            String sourceResumeText
    ) {
        if (response == null) {
            return;
        }

        List<String> sourceProjectTitles = extractProjectTitlesFromSource(sourceResumeText);
        List<ProjectItem> projects = response.getProjects() == null
                ? new ArrayList<>()
                : new ArrayList<>(response.getProjects());
        List<ExperienceItem> experience = response.getExperience() == null
                ? new ArrayList<>()
                : new ArrayList<>(response.getExperience());

        java.util.LinkedHashSet<String> assignedProjectTitles = new java.util.LinkedHashSet<>();

        // First correct project names using exact headings from the source resume.
        for (ProjectItem project : projects) {
            if (project == null) {
                continue;
            }

            String currentName = normalizeText(project.getName());
            String matchingTitle = findBestSourceProjectTitle(
                    currentName + " " + String.join(" ", normalizeStringList(project.getBullets())),
                    sourceProjectTitles,
                    assignedProjectTitles
            );

            if (currentName.isBlank() || isLikelyEmploymentTitle(currentName)) {
                if (matchingTitle == null) {
                    matchingTitle = firstUnassignedProjectTitle(sourceProjectTitles, assignedProjectTitles);
                }
                if (matchingTitle != null) {
                    project.setName(matchingTitle);
                }
            }

            String resolvedName = normalizeText(project.getName());
            if (!resolvedName.isBlank()) {
                assignedProjectTitles.add(normalizedKey(resolvedName));
            }
        }

        List<ExperienceItem> correctedExperience = new ArrayList<>();

        for (ExperienceItem item : experience) {
            if (item == null) {
                continue;
            }

            String title = normalizeText(item.getJobTitle());
            String company = normalizeText(item.getCompany());
            String content = title + " " + company + " "
                    + String.join(" ", normalizeStringList(item.getBullets()));

            boolean invalidCompany = isInvalidCompanyName(company, title);
            boolean duplicateEmployment = hasSameEmploymentSignature(item, correctedExperience);

            // NVIDIA/Groq can return a project as a generic job title with no real company.
            // Compare the bullets to Projects and remove this duplicate Experience entry.
            ProjectItem matchingProject = findBestProjectByBulletOverlap(item, projects);
            String matchedSourceTitle = findBestSourceProjectTitle(
                    content,
                    sourceProjectTitles,
                    assignedProjectTitles
            );

            boolean genericProjectDisguisedAsRole = isLikelyEmploymentTitle(title)
                    && invalidCompany
                    && !normalizeStringList(item.getBullets()).isEmpty();

            boolean shouldMoveToProjects = genericProjectDisguisedAsRole
                    && (matchingProject != null || matchedSourceTitle != null || !sourceProjectTitles.isEmpty());

            // A repeated employment signature with matching project bullets is also a duplicate.
            if (!shouldMoveToProjects && duplicateEmployment
                    && (matchingProject != null || matchedSourceTitle != null)) {
                shouldMoveToProjects = true;
            }

            if (shouldMoveToProjects) {
                ProjectItem destination = matchingProject;

                if (destination == null) {
                    String projectTitle = matchedSourceTitle;
                    if (projectTitle == null || projectTitle.isBlank()) {
                        projectTitle = firstUnassignedProjectTitle(sourceProjectTitles, assignedProjectTitles);
                    }

                    if (projectTitle != null && !projectTitle.isBlank()) {
                        destination = findProjectByName(projects, projectTitle);
                        if (destination == null) {
                            destination = new ProjectItem();
                            destination.setName(projectTitle);
                            projects.add(destination);
                        }
                    }
                }

                if (destination != null) {
                    mergeExperienceIntoProject(destination, item);
                    assignedProjectTitles.add(normalizedKey(destination.getName()));
                }

                // Do not add this item to Professional Experience.
                continue;
            }

            correctedExperience.add(item);
        }

        response.setExperience(normalizeExperience(correctedExperience));
        response.setProjects(normalizeProjects(projects));
    }

    private ProjectItem findBestProjectByBulletOverlap(
            ExperienceItem experienceItem,
            List<ProjectItem> projects
    ) {
        if (experienceItem == null || projects == null || projects.isEmpty()) {
            return null;
        }

        java.util.Set<String> experienceTokens = meaningfulTokens(
                String.join(" ", normalizeStringList(experienceItem.getBullets()))
        );
        if (experienceTokens.isEmpty()) {
            return null;
        }

        ProjectItem bestProject = null;
        int bestScore = 0;

        for (ProjectItem project : projects) {
            if (project == null) {
                continue;
            }

            java.util.Set<String> projectTokens = meaningfulTokens(
                    normalizeText(project.getName()) + " "
                            + String.join(" ", normalizeStringList(project.getBullets()))
            );

            int score = 0;
            for (String token : experienceTokens) {
                if (projectTokens.contains(token)) {
                    score++;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestProject = project;
            }
        }

        return bestScore >= 3 ? bestProject : null;
    }

    private java.util.Set<String> meaningfulTokens(String value) {
        java.util.Set<String> tokens = new java.util.LinkedHashSet<>();
        String normalized = normalizedKey(value);

        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 4 && !isGenericProjectWord(token)
                    && !token.equals("using") && !token.equals("with")
                    && !token.equals("that") && !token.equals("this")
                    && !token.equals("from") && !token.equals("into")) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private void mergeExperienceIntoProject(ProjectItem project, ExperienceItem experienceItem) {
        if (project == null || experienceItem == null) {
            return;
        }

        if (normalizeText(project.getStartDate()).isBlank()) {
            project.setStartDate(normalizeText(experienceItem.getStartDate()));
        }
        if (normalizeText(project.getEndDate()).isBlank()) {
            project.setEndDate(normalizeText(experienceItem.getEndDate()));
        }

        List<String> mergedBullets = new ArrayList<>();
        mergedBullets.addAll(normalizeStringList(project.getBullets()));
        mergedBullets.addAll(normalizeStringList(experienceItem.getBullets()));
        project.setBullets(normalizeStringList(mergedBullets));
    }

    private List<String> extractProjectTitlesFromSource(String sourceResumeText) {
        List<String> titles = new ArrayList<>();
        String source = normalizeSourceResumeText(sourceResumeText);
        boolean insideProjects = false;

        for (String rawLine : source.split("\\n")) {
            String line = normalizeText(rawLine);
            if (line.isBlank()) {
                continue;
            }

            String upper = line.toUpperCase(java.util.Locale.ROOT);
            if (upper.matches("^(FEATURED\\s+)?PROJECTS?\\s*$")) {
                insideProjects = true;
                continue;
            }

            if (insideProjects && isSourceSectionHeading(upper)) {
                insideProjects = false;
            }

            String explicitTitle = extractExplicitProjectHeading(line);
            if (!explicitTitle.isBlank()) {
                addDistinctProjectTitle(titles, explicitTitle);
                continue;
            }

            if (insideProjects && looksLikeProjectHeading(line)) {
                addDistinctProjectTitle(titles, stripTrailingDateRange(line));
            }
        }

        return titles;
    }

    private boolean isSourceSectionHeading(String upper) {
        return upper.matches("^(PROFESSIONAL\\s+EXPERIENCE|WORK\\s+EXPERIENCE|EDUCATION|CERTIFICATIONS?|ACHIEVEMENTS?|AWARDS?|SKILLS?|KEY\\s+SKILLS|SUMMARY|PROFESSIONAL\\s+SUMMARY).*$");
    }

    private String extractExplicitProjectHeading(String line) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)^(?:FEATURED\\s+)?PROJECT\\s*\\d*\\s*[:\\-]\\s*(.+)$")
                .matcher(line);

        if (!matcher.matches()) {
            return "";
        }

        return stripTrailingDateRange(matcher.group(1));
    }

    private boolean looksLikeProjectHeading(String line) {
        String lower = line.toLowerCase(java.util.Locale.ROOT);
        if (line.startsWith("•") || lower.startsWith("tech stack") || line.length() > 120) {
            return false;
        }

        if (lower.matches(".*\\b(implemented|developed|designed|engineered|built|architected|configured|integrated|optimized|executed|delivered)\\b.*")) {
            return false;
        }

        return lower.contains("application")
                || lower.contains("platform")
                || lower.contains("system")
                || lower.contains("project")
                || lower.contains("chat")
                || lower.contains("booking")
                || lower.contains("e-commerce");
    }

    private String stripTrailingDateRange(String value) {
        String cleanValue = normalizeText(value);
        return cleanValue
                .replaceAll("(?i)\\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\.?\\s*'?\\d{2,4}(?:\\s*(?:-|–|to)\\s*(?:present|(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\.?\\s*'?\\d{2,4}))?\\s*$", "")
                .trim();
    }

    private void addDistinctProjectTitle(List<String> titles, String title) {
        String cleanTitle = normalizeText(title);
        if (cleanTitle.isBlank()) {
            return;
        }

        String cleanKey = normalizedKey(cleanTitle);
        for (String existing : titles) {
            if (normalizedKey(existing).equals(cleanKey)) {
                return;
            }
        }
        titles.add(cleanTitle);
    }

    private String findBestSourceProjectTitle(
            String candidateContent,
            List<String> sourceProjectTitles,
            java.util.Set<String> assignedProjectTitles
    ) {
        String candidateKey = normalizedKey(candidateContent);
        if (candidateKey.isBlank()) {
            return null;
        }

        String bestTitle = null;
        int bestScore = 0;

        for (String title : sourceProjectTitles) {
            String titleKey = normalizedKey(title);
            int score = 0;

            if (candidateKey.contains(titleKey) || titleKey.contains(candidateKey)) {
                score += 10;
            }

            for (String word : titleKey.split("\\s+")) {
                if (word.length() >= 4 && !isGenericProjectWord(word)
                        && candidateKey.contains(word)) {
                    score++;
                }
            }

            if (assignedProjectTitles.contains(titleKey)) {
                score--;
            }

            if (score > bestScore) {
                bestScore = score;
                bestTitle = title;
            }
        }

        return bestScore >= 1 ? bestTitle : null;
    }

    private boolean isGenericProjectWord(String word) {
        return word.equals("project")
                || word.equals("application")
                || word.equals("platform")
                || word.equals("system")
                || word.equals("software")
                || word.equals("powered")
                || word.equals("full")
                || word.equals("stack");
    }

    private String firstUnassignedProjectTitle(
            List<String> sourceProjectTitles,
            java.util.Set<String> assignedProjectTitles
    ) {
        for (String title : sourceProjectTitles) {
            if (!assignedProjectTitles.contains(normalizedKey(title))) {
                return title;
            }
        }
        return null;
    }

    private ProjectItem findProjectByName(List<ProjectItem> projects, String name) {
        String expectedKey = normalizedKey(name);
        for (ProjectItem project : projects) {
            if (project != null && normalizedKey(project.getName()).equals(expectedKey)) {
                return project;
            }
        }
        return null;
    }

    private boolean hasSameEmploymentSignature(
            ExperienceItem item,
            List<ExperienceItem> earlierItems
    ) {
        String signature = normalizedKey(item.getJobTitle()) + "|" + normalizedKey(item.getCompany());
        if (signature.equals("|")) {
            return false;
        }

        for (ExperienceItem earlier : earlierItems) {
            String earlierSignature = normalizedKey(earlier.getJobTitle()) + "|"
                    + normalizedKey(earlier.getCompany());
            if (signature.equals(earlierSignature)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInvalidCompanyName(String company, String jobTitle) {
        String companyKey = normalizedKey(company);
        String titleKey = normalizedKey(jobTitle);

        if (companyKey.isBlank() || companyKey.equals(titleKey)) {
            return true;
        }

        return companyKey.matches("^(engineer|developer|analyst|manager|consultant|specialist|architect|administrator|associate|executive|company|organization|unknown|na|n a|personal project|client project|self project|freelance project)$");
    }

    private boolean isLikelyEmploymentTitle(String value) {
        String lower = normalizeText(value).toLowerCase(java.util.Locale.ROOT);
        return lower.matches(".*\\b(developer|engineer|analyst|manager|consultant|specialist|architect|administrator|intern|associate|executive)\\b.*");
    }

    private String normalizedKey(String value) {
        return normalizeText(value)
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private List<CertificationItem> normalizeCertifications(List<CertificationItem> certifications) {
        if (certifications == null) {
            return new ArrayList<>();
        }

        List<CertificationItem> normalized = new ArrayList<>();

        for (CertificationItem item : certifications) {
            if (item == null) {
                continue;
            }

            CertificationItem cleanItem = new CertificationItem();
            cleanItem.setTitle(normalizeText(item.getTitle()));
            cleanItem.setIssuer(normalizeText(item.getIssuer()));
            cleanItem.setYear(normalizeText(item.getYear()));
            cleanItem.setDetails(normalizeText(item.getDetails()));

            if (!cleanItem.getTitle().isBlank() || !cleanItem.getDetails().isBlank()) {
                normalized.add(cleanItem);
            }
        }

        return normalized;
    }

    private List<AchievementItem> normalizeAchievements(List<AchievementItem> achievements) {
        if (achievements == null) {
            return new ArrayList<>();
        }

        List<AchievementItem> normalized = new ArrayList<>();

        for (AchievementItem item : achievements) {
            if (item == null) {
                continue;
            }

            AchievementItem cleanItem = new AchievementItem();
            cleanItem.setTitle(normalizeText(item.getTitle()));
            cleanItem.setDetails(normalizeText(item.getDetails()));

            if (!cleanItem.getTitle().isBlank() || !cleanItem.getDetails().isBlank()) {
                normalized.add(cleanItem);
            }
        }

        return normalized;
    }

    private List<EducationItem> normalizeEducation(List<EducationItem> education) {
        if (education == null) {
            return new ArrayList<>();
        }

        List<EducationItem> normalized = new ArrayList<>();

        for (EducationItem item : education) {
            if (item == null) {
                continue;
            }

            EducationItem cleanItem = new EducationItem();
            cleanItem.setDegree(normalizeText(item.getDegree()));
            cleanItem.setInstitution(normalizeText(item.getInstitution()));
            cleanItem.setLocation(normalizeText(item.getLocation()));
            cleanItem.setStartDate(normalizeText(item.getStartDate()));
            cleanItem.setEndDate(normalizeText(item.getEndDate()));
            cleanItem.setDetails(normalizeText(item.getDetails()));

            if (!cleanItem.getDegree().isBlank() || !cleanItem.getInstitution().isBlank()) {
                normalized.add(cleanItem);
            }
        }

        return normalized;
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return values.stream()
                .map(this::normalizeText)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> flattenSkills(List<SkillGroup> skillGroups) {
        List<String> skills = new ArrayList<>();

        for (SkillGroup group : skillGroups) {
            skills.addAll(group.getItems());
        }

        return skills;
    }

    private List<String> flattenExperienceBullets(List<ExperienceItem> experience) {
        List<String> bullets = new ArrayList<>();

        for (ExperienceItem item : experience) {
            bullets.addAll(item.getBullets());
        }

        return bullets;
    }

    private List<String> flattenProjectBullets(List<ProjectItem> projects) {
        List<String> bullets = new ArrayList<>();

        for (ProjectItem item : projects) {
            bullets.addAll(item.getBullets());
        }

        return bullets;
    }

    private String buildEducationText(List<EducationItem> education) {
        StringBuilder builder = new StringBuilder();

        for (EducationItem item : education) {
            appendLine(builder, item.getDegree());

            String institutionLine = joinNonBlank(
                    " | ",
                    item.getInstitution(),
                    item.getLocation()
            );
            appendLine(builder, institutionLine);

            String dateLine = joinNonBlank(
                    " - ",
                    item.getStartDate(),
                    item.getEndDate()
            );
            appendLine(builder, dateLine);
            appendLine(builder, item.getDetails());

            if (!builder.isEmpty()) {
                builder.append("\n");
            }
        }

        return builder.toString().trim();
    }

    private String buildFullResumeText(ResumeBuilderResponse response) {
        StringBuilder builder = new StringBuilder();

        ResumeBasics basics = response.getBasics();

        appendLine(builder, basics.getFullName());
        appendLine(builder, basics.getHeadline());

        String contactLine = joinNonBlank(
                " | ",
                basics.getEmail(),
                basics.getPhone(),
                basics.getLocation(),
                basics.getLinkedin()
        );
        appendLine(builder, contactLine);

        if (!normalizeText(response.getProfessionalSummary()).isBlank()) {
            appendSectionTitle(builder, "PROFESSIONAL SUMMARY");
            appendLine(builder, response.getProfessionalSummary());
        }

        if (!response.getSkillGroups().isEmpty()) {
            appendSectionTitle(builder, "KEY SKILLS");
            for (SkillGroup group : response.getSkillGroups()) {
                appendLine(builder, group.getCategory() + ": " + String.join(", ", group.getItems()));
            }
        }

        if (!response.getExperience().isEmpty()) {
            appendSectionTitle(builder, "PROFESSIONAL EXPERIENCE");
            for (ExperienceItem item : response.getExperience()) {
                String titleLine = joinNonBlank(" | ", item.getJobTitle(), item.getCompany());
                String detailLine = joinNonBlank(
                        " | ",
                        item.getLocation(),
                        joinNonBlank(" - ", item.getStartDate(), item.getEndDate())
                );

                appendLine(builder, titleLine);
                appendLine(builder, detailLine);

                for (String bullet : item.getBullets()) {
                    appendLine(builder, "• " + bullet);
                }

                builder.append("\n");
            }
        }

        if (!response.getProjects().isEmpty()) {
            appendSectionTitle(builder, "PROJECTS");
            for (ProjectItem item : response.getProjects()) {
                appendLine(builder, item.getName());
                appendLine(builder, joinNonBlank(" - ", item.getStartDate(), item.getEndDate()));

                for (String bullet : item.getBullets()) {
                    appendLine(builder, "• " + bullet);
                }

                builder.append("\n");
            }
        }

        if (!response.getCertifications().isEmpty()) {
            appendSectionTitle(builder, "CERTIFICATIONS");
            for (CertificationItem item : response.getCertifications()) {
                appendLine(builder, joinNonBlank(" | ", item.getTitle(), item.getIssuer(), item.getYear()));
                appendLine(builder, item.getDetails());
            }
        }

        if (!response.getAchievements().isEmpty()) {
            appendSectionTitle(builder, "ACHIEVEMENTS");
            for (AchievementItem item : response.getAchievements()) {
                appendLine(builder, "• " + joinNonBlank(" - ", item.getTitle(), item.getDetails()));
            }
        }

        if (!response.getEducation().isEmpty()) {
            appendSectionTitle(builder, "EDUCATION");
            appendLine(builder, response.getEducationText());
        }

        return builder.toString().trim();
    }

    private void appendSectionTitle(StringBuilder builder, String title) {
        if (!builder.isEmpty()) {
            builder.append("\n\n");
        }

        builder.append(title).append("\n");
    }

    private void appendLine(StringBuilder builder, String value) {
        String text = normalizeText(value);

        if (!text.isBlank()) {
            builder.append(text).append("\n");
        }
    }

    private String joinNonBlank(String delimiter, String... values) {
        StringJoiner joiner = new StringJoiner(delimiter);

        for (String value : values) {
            String text = normalizeText(value);

            if (!text.isBlank()) {
                joiner.add(text);
            }
        }

        return joiner.toString();
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return values.stream()
                .map(this::normalizeText)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String normalizeSourceResumeText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\f\\x0B ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
