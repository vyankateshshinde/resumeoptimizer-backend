package com.vyankatesh.resumeoptimizer.resume;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<ResumeEntity, Long> {

    // Fetch latest resume for a user
    Optional<ResumeEntity> findTopByEmailOrderByIdDesc(String email);

    // Dashboard Analytics
    long countByEmail(String email);
}