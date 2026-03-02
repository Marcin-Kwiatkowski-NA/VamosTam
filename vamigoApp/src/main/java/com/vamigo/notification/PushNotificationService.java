package com.vamigo.notification;

import java.util.Map;

public interface PushNotificationService {

    void sendToUser(Long userId, String title, String body, Map<String, String> data);
}
