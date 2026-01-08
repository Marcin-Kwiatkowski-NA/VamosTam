package com.blablatwo.traveler;

import jakarta.persistence.EntityExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TravelerServiceImpl implements TravelerService {

    private final TravelerRepository travelerRepository;
    private final TravelerMapper travelerMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public TravelerServiceImpl(TravelerRepository travelerRepository, TravelerMapper travelerMapper, PasswordEncoder passwordEncoder) {
        this.travelerRepository = travelerRepository;
        this.travelerMapper = travelerMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Traveler traveler = travelerRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Traveler not found with username: " + username));

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(traveler.getRole().getAuthority()));

        return User.builder()
                .username(traveler.getUsername())
                .password(traveler.getPassword())
                .authorities(authorities)
                .disabled(traveler.getEnabled() == 0)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Traveler> getById(Long id) {
        return travelerRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Traveler> getByUsername(String username) {
        return travelerRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Traveler> getAllTravelers() {
        return travelerRepository.findAll();
    }

    @Override
    @Transactional
    public TravelerResponseDto create(TravelerCreationDto travelerDto) {
        if (travelerRepository.findByUsername(travelerDto.username()).isPresent()) {
            throw new EntityExistsException("Traveler with username '" + travelerDto.username() + "' already exists.");
        }
        if (travelerRepository.findByEmail(travelerDto.email()).isPresent()) {
            throw new EntityExistsException("Traveler with email '" + travelerDto.email() + "' already exists.");
        }

        var newTravelerEntity = travelerMapper.travelerCreationDtoToEntity(travelerDto);
        newTravelerEntity.setPassword(passwordEncoder.encode(travelerDto.password()));

        return travelerMapper.travelerEntityToTravelerResponseDto(
                travelerRepository.save(newTravelerEntity));
    }

    @Override
    @Transactional
    public TravelerResponseDto update(TravelerCreationDto travelerDto, Long id) {
        var existingTraveler = travelerRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Traveler with ID " + id + " not found."));

        travelerMapper.update(existingTraveler, travelerDto);

        return travelerMapper.travelerEntityToTravelerResponseDto(
                travelerRepository.save(existingTraveler));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (travelerRepository.existsById(id)) {
            travelerRepository.deleteById(id);
        } else {
            throw new UsernameNotFoundException("Traveler with ID " + id + " not found.");
        }
    }
}