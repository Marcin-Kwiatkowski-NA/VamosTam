package com.vamigo.searchalert.dto;

public record UpdateNotificationPreferenceRequest(
        Boolean searchAlertsPushEnabled,
        Boolean searchAlertsEmailEnabled
) {
}
