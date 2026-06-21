package com.vyankatesh.resumeoptimizer.prompteditor.util;

import com.vyankatesh.resumeoptimizer.prompteditor.enums.PromptIntent;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilderUtil {

    public String buildPrompt(PromptIntent intent,
                              String currentResumeText,
                              String jobDescription,
                              String userPrompt) {

        String baseRules = """
                You are an expert ATS-friendly resume editor.

                Strict rules:
                1. Do not add fake skills, tools, projects, companies, education, certifications, or experience.
                2. Do not invent exact metrics unless the user gives the number.
                3. If user asks to remove a skill, remove it from all resume sections.
                4. Keep resume professional, ATS-friendly, and aligned with the job description.
                5. Preserve the original resume structure as much as possible.
                6. Return only the final updated resume text. Do not add explanation outside the resume.
                """;

        String taskInstruction = switch (intent) {
            case REMOVE_SKILL -> """
                    Task:
                    Remove the skill, tool, or technology mentioned in the user prompt from the resume.
                    Remove it from Skills, Summary, Experience, Projects, and any other section.
                    Do not replace it with another skill unless it already exists in the resume.
                    """;

            case ADD_SKILL -> """
                    Task:
                    Add the skill or keyword requested by the user only if it is already supported by the resume context.
                    If it is not supported, add it only in a safe way such as 'Exposure to' or do not add it.
                    Keep the resume honest and ATS-friendly.
                    """;

            case REMOVE_WEAK_LINES -> """
                    Task:
                    Remove or rewrite weak, generic, repeated, or low-impact resume lines.
                    Improve clarity, action verbs, and business impact.
                    """;

            case IMPROVE_SUMMARY -> """
                    Task:
                    Improve only the professional summary section.
                    Make it stronger, concise, ATS-friendly, and aligned with the job description.
                    """;

            case IMPROVE_EXPERIENCE -> """
                    Task:
                    Improve the experience section.
                    Use stronger action verbs, backend/frontend keywords, and job-description alignment.
                    Do not add fake responsibilities.
                    """;

            case IMPROVE_PROJECTS -> """
                    Task:
                    Improve the projects section.
                    Make project bullets stronger, technical, ATS-friendly, and impact-oriented.
                    """;

            case ATS_OPTIMIZATION -> """
                    Task:
                    Optimize the resume according to the job description.
                    Improve keyword alignment naturally.
                    Do not keyword-stuff or add skills not supported by the resume.
                    """;

            case ADD_METRICS -> """
                    Task:
                    Add impact-driven wording to resume bullets.
                    If the user provides exact metrics, use them.
                    If no exact numbers are provided, use non-fake wording like:
                    'improved performance', 'enhanced scalability', 'reduced manual effort', 'improved maintainability'.
                    """;

            case MAKE_CONCISE -> """
                    Task:
                    Make the resume more concise.
                    Reduce repeated lines, shorten long bullets, and keep the strongest ATS-friendly content.
                    """;

            default -> """
                    Task:
                    Apply the user's requested changes safely and professionally.
                    Keep the resume ATS-friendly and honest.
                    """;
        };

        return """
                %s

                %s

                Current Resume:
                %s

                Job Description:
                %s

                User Prompt:
                %s
                """.formatted(baseRules, taskInstruction, currentResumeText, jobDescription, userPrompt);
    }
}