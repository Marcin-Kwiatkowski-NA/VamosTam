package com.blablatwo.traveler;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

public interface TravelerService extends UserDetailsService {
    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    Optional<Traveler> getById(Long id);
    Optional<Traveler> getByUsername(String username);
    List<Traveler> getAllTravelers();
    TravelerResponseDto create(TravelerCreationDto travelerDto);
    TravelerResponseDto update(TravelerCreationDto travelerDto, Long id);
    void delete(Long id);
}