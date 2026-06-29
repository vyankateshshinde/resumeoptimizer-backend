package com.vyankatesh.resumeoptimizer.resumebuilder.dto;

import java.util.ArrayList;
import java.util.List;

public class ResumeBuilderResponse {

    private String templateName;
    private ResumeBasics basics = new ResumeBasics();
    private String professionalSummary;

    private List<SkillGroup> skillGroups = new ArrayList<>();
    private List<ExperienceItem> experience = new ArrayList<>();
    private List<ProjectItem> projects = new ArrayList<>();
    private List<CertificationItem> certifications = new ArrayList<>();
    private List<AchievementItem> achievements = new ArrayList<>();
    private List<EducationItem> education = new ArrayList<>();

    private String fullResumeText;

    // Compatibility fields used by existing version/history and frontend logic.
    private List<String> skills = new ArrayList<>();
    private List<String> experienceBullets = new ArrayList<>();
    private List<String> projectBullets = new ArrayList<>();
    private String educationText;
    private List<ResumeSectionItem> sectionLayout = new ArrayList<>();

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public ResumeBasics getBasics() { return basics; }
    public void setBasics(ResumeBasics basics) { this.basics = basics; }

    public String getProfessionalSummary() { return professionalSummary; }
    public void setProfessionalSummary(String professionalSummary) { this.professionalSummary = professionalSummary; }

    public List<SkillGroup> getSkillGroups() { return skillGroups; }
    public void setSkillGroups(List<SkillGroup> skillGroups) { this.skillGroups = skillGroups; }

    public List<ExperienceItem> getExperience() { return experience; }
    public void setExperience(List<ExperienceItem> experience) { this.experience = experience; }

    public List<ProjectItem> getProjects() { return projects; }
    public void setProjects(List<ProjectItem> projects) { this.projects = projects; }

    public List<CertificationItem> getCertifications() { return certifications; }
    public void setCertifications(List<CertificationItem> certifications) { this.certifications = certifications; }

    public List<AchievementItem> getAchievements() { return achievements; }
    public void setAchievements(List<AchievementItem> achievements) { this.achievements = achievements; }

    public List<EducationItem> getEducation() { return education; }
    public void setEducation(List<EducationItem> education) { this.education = education; }

    public String getFullResumeText() { return fullResumeText; }
    public void setFullResumeText(String fullResumeText) { this.fullResumeText = fullResumeText; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getExperienceBullets() { return experienceBullets; }
    public void setExperienceBullets(List<String> experienceBullets) { this.experienceBullets = experienceBullets; }

    public List<String> getProjectBullets() { return projectBullets; }
    public void setProjectBullets(List<String> projectBullets) { this.projectBullets = projectBullets; }

    public String getEducationText() { return educationText; }
    public void setEducationText(String educationText) { this.educationText = educationText; }

    public List<ResumeSectionItem> getSectionLayout() { return sectionLayout; }
    public void setSectionLayout(List<ResumeSectionItem> sectionLayout) {
        this.sectionLayout = sectionLayout == null ? new ArrayList<>() : sectionLayout;
    }
}
