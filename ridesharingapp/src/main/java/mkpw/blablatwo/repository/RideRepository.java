package mkpw.blablatwo.repository;

import mkpw.blablatwo.entity.RideEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RideRepository extends CrudRepository<RideEntity, UUID> {
    @Query("select c from RideEntity c join c.driver u where u.id = :driverId")
    Iterable<RideEntity> findByDriverId(@Param("driverId") UUID driverID);
}
