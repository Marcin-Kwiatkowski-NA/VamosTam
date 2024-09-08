package com.mkpw.blaBlaTwo.repository;

import com.mkpw.blaBlaTwo.entity.RideEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.inject.Qualifier;
import java.util.Optional;
import java.util.UUID;

public interface RideRepository extends CrudRepository<RideEntity, UUID> {
    @Query("select c from RideEntity c join c.user u where u.id = :driverID")
    Iterable<RideEntity> findByDriverId(@Param("driverId") UUID driverID);
}
