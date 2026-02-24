package com.blablatwo.review.dto;

import com.blablatwo.review.ReviewTag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitReviewRequest(
        @NotNull @Min(1) @Max(5) Integer stars,
        @Size(max = 500) String comment,
        List<ReviewTag> tags
) {
}
