package com.vyankatesh.resumeoptimizer.resume;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<ResumeEntity, Long> {

    Optional<ResumeEntity> findTopByEmailOrderByIdDesc(String email);

    List<ResumeEntity> findByEmailOrderByIdDesc(String email);

    long countByEmail(String email);
}