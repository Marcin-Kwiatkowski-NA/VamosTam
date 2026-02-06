package com.blablatwo.dto;

public record UserCardDto(
        Long id,
        String name,
        Double rating,
        Integer completedRides
) {
}
