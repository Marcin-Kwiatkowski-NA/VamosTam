package com.vamigo.searchalert.dto;

public record NotificationPreferenceDto(
        boolean searchAlertsPushEnabled,
        boolean searchAlertsEmailEnabled
) {
}
