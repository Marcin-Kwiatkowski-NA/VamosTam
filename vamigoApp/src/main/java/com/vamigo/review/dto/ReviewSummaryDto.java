package com.vamigo.review.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ReviewSummaryDto(
        double averageRating,
        int totalReviews,
        List<Integer> distribution // [count1star, count2star, ..., count5star]
) {}
