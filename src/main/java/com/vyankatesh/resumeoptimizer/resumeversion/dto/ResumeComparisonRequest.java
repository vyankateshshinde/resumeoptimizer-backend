package com.vyankatesh.resumeoptimizer.resumeversion.dto;

public class ResumeComparisonRequest {

    private Long versionId1;
    private Long versionId2;

    public Long getVersionId1() {
        return versionId1;
    }

    public void setVersionId1(Long versionId1) {
        this.versionId1 = versionId1;
    }

    public Long getVersionId2() {
        return versionId2;
    }

    public void setVersionId2(Long versionId2) {
        this.versionId2 = versionId2;
    }
}