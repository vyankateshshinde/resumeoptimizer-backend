package com.vyankatesh.resumeoptimizer.jobfinder.model;

public enum ExperienceRequirementType {

    /**
     * The job description clearly states that the experience
     * is mandatory, required, minimum, or must-have.
     */
    REQUIRED,

    /**
     * The experience is preferred, desirable, optional,
     * or mentioned as an advantage.
     */
    PREFERRED,

    /**
     * The job description does not contain a reliable
     * experience requirement.
     */
    NOT_SPECIFIED,

    /**
     * The job description contains conflicting or unclear
     * experience requirements that need verification.
     */
    AMBIGUOUS
}