package com.vamigo.searchalert.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleAlertRequest(
        @NotNull Boolean active
) {
}
