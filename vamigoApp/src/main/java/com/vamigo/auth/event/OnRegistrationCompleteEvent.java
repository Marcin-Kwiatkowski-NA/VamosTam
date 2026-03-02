package com.vamigo.auth.event;

import com.vamigo.user.UserAccount;

import java.util.Locale;

public record OnRegistrationCompleteEvent(UserAccount user, Locale locale) {}
