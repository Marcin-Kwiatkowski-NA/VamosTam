package com.blablatwo.dto;

import jakarta.validation.constraints.Size;

public record CancellationRequest(
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason
) {
}
