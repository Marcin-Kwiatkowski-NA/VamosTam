package com.vamigo.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "carrier_profile")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
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

    @Column(name = "website_url", length = 500, nullable = true)
    private String websiteUrl;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Builder.Default
    @Column(name = "booking_enabled", nullable = false, columnDefinition = "boolean not null default true")
    private boolean bookingEnabled = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void updateDetails(CarrierProfileDetails details) {
        if (details.companyName() != null) {
            this.companyName = details.companyName();
        }
        if (details.slug() != null) {
            this.slug = details.slug();
        }
    }

    public void updateWebsite(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public void enableBooking() {
        this.bookingEnabled = true;
    }

    public void disableBooking() {
        this.bookingEnabled = false;
    }
}
