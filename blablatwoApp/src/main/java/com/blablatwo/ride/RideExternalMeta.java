package com.blablatwo.ride;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ride_external_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideExternalMeta {

    @Id
    private Long rideId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "ride_id")
    private Ride ride;

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
