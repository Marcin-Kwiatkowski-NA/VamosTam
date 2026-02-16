package com.blablatwo.auth.event;

import com.blablatwo.user.UserAccount;

import java.util.Locale;

public record OnRegistrationCompleteEvent(UserAccount user, Locale locale) {}
