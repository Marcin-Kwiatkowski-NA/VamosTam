package com.vamigo.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarrierProfileRepository extends JpaRepository<CarrierProfile, Long> {

    boolean existsByNip(String nip);
}
