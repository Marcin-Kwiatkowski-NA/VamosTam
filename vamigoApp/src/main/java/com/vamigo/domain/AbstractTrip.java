package com.vamigo.domain;

import com.vamigo.ride.RideSource;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public abstract class AbstractTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RideSource source = RideSource.INTERNAL;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 32)
    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @LastModifiedDate
    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;

    @Version
    private int version;

    protected void changeStatus(Status status) {
        this.status = status;
    }

    protected void applyCommonDetails(String description, String contactPhone, Currency currency) {
        this.description = description;
        this.contactPhone = contactPhone;
        this.currency = currency;
    }

    protected void touchLastModified(Instant now) {
        this.lastModified = now;
    }
}
