package com.blablatwo.auth;

import com.blablatwo.traveler.Role;

public record AppPrincipal(Long travelerId, String email, Role role) {}
