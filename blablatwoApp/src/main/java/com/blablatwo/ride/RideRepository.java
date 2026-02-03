package com.blablatwo.ride;

import com.blablatwo.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long>, JpaSpecificationExecutor<Ride> {

    List<Ride> findByPassengersContaining(UserAccount passenger);

    List<Ride> findByDriverId(Long driverId);

    @Query("""
        select case when count(p) > 0 then true else false end
        from Ride r join r.passengers p
        where r.id = :rideId and p.id = :passengerId
        """)
    boolean existsPassenger(@Param("rideId") Long rideId,
                            @Param("passengerId") Long passengerId);
}
