package com.blablatwo.auth.event;

import com.blablatwo.user.UserAccount;

public record OnRegistrationCompleteEvent(UserAccount user) {}
