package com.vyankatesh.resumeoptimizer.ats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AtsHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private Long resumeId;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String jobDescription;

    private int skillScore;

    private int keywordScore;

    private int finalScore;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String feedback;

    private LocalDateTime createdAt;
}