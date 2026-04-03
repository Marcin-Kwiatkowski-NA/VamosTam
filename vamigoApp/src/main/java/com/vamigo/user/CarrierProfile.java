package com.vamigo.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "carrier_profile")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarrierProfile {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private UserAccount account;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "nip", nullable = true, unique = true, length = 10)
    private String nip;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "booking_enabled", nullable = false, columnDefinition = "boolean not null default true")
    @Builder.Default
    private boolean bookingEnabled = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
