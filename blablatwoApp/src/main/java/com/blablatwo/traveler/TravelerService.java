package com.blablatwo.traveler;

import java.util.List;
import java.util.Optional;

public interface TravelerService {

    Optional<Traveler> getById(Long id);

    Optional<Traveler> getByUsername(String username);

    List<Traveler> getAllTravelers();

    TravelerResponseDto create(TravelerCreationDto travelerDto);

    TravelerResponseDto update(TravelerCreationDto travelerDto, Long id);

    void delete(Long id);
}