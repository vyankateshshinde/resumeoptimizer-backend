package com.vyankatesh.resumeoptimizer.resumeversion.repository;

import com.vyankatesh.resumeoptimizer.resumeversion.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {

    List<ResumeVersion> findByResumeId(Long resumeId);
}