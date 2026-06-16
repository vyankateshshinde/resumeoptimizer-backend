package com.vyankatesh.resumeoptimizer.resume;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @JsonIgnore
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] data;

    private String email;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;
}