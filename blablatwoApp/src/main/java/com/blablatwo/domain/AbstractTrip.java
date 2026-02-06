package com.blablatwo.domain;

import com.blablatwo.ride.RideSource;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AbstractTrip {

    @Id
    @GeneratedValue
    private Long id;

    @Embedded
    @NotNull
    private Segment segment;

    @Embedded
    @NotNull
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RideSource source = RideSource.INTERNAL;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Column(name = "last_modified")
    private Instant lastModified;

    @Version
    private int version;
}
