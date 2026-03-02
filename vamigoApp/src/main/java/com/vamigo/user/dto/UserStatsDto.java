package com.vamigo.user.dto;

import java.math.BigDecimal;

public record UserStatsDto(
    int ridesGiven,
    int ridesTaken,
    BigDecimal ratingAvg,
    int ratingCount
) {}
