package com.vamigo.user.dto;

import com.vamigo.dto.UserCardDto;
import com.vamigo.ride.dto.RideResponseDto;

import java.util.List;

public record CarrierPublicPageDto(
        CarrierProfileDto carrier,
        UserCardDto userCard,
        List<RideResponseDto> rides,
        int totalRides,
        int totalPages,
        int currentPage
) {
}
