package com.blablatwo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AbstractExternalMeta {

    @Id
    private Long id;

    @NotBlank
    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(name = "author_name", length = 100)
    private String authorName;
}
