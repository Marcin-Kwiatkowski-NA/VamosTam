package com.blablatwo.user;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStats {

    @Builder.Default
    private int ridesGiven = 0;

    @Builder.Default
    private int ridesTaken = 0;

    @Builder.Default
    private int ratingSum = 0;

    @Builder.Default
    private int ratingCount = 0;
}
