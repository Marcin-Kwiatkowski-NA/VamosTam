package com.vamigo.user.dto;

import com.vamigo.location.LocationDto;

import java.util.List;

public record CarrierDirectionsDto(List<LocationDto> locations) {
}
