package com.blablatwo.traveler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TravelerRepository extends JpaRepository<Traveler, Long> {
    Optional<Traveler> findByUsername(String username);
    Optional<Traveler> findByEmail(String email);
}