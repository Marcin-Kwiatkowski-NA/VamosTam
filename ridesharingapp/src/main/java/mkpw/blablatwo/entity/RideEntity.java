package mkpw.blablatwo.entity;

import mkpw.blaBlaTwo.model.Ride.StatusEnum;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "rides")
public class RideEntity {
    @Id
    @GeneratedValue
    @Column (name = "ID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "DRIVER_ID", referencedColumnName = "ID")
    private UserEntity driver;

    @ManyToOne
    @JoinColumn(name = "START_CITY_ID", referencedColumnName = "ID")
    private CityEntity startCity;

    @ManyToOne
    @JoinColumn(name = "DESTINATION_CITY_ID", referencedColumnName = "ID")
    private CityEntity destinationCity;

    @Column(name = "PRICE")
    private BigDecimal price;

    @Column(name = "PET_FRIENDLY")
    private boolean petFriendly;

    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @Column(name = "DEPARTURE_TIME")
    private Timestamp departureTime;

    public UUID getId() {
        return id;
    }

    public RideEntity setId(UUID id) {
        this.id = id;
        return this;
    }

    public UserEntity getDriver() {
        return driver;
    }

    public RideEntity setDriver(UserEntity driver) {
        this.driver = driver;
        return this;
    }

    public CityEntity getStartCity() {
        return startCity;
    }

    public RideEntity setStartCity(CityEntity startCity) {
        this.startCity = startCity;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public RideEntity setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public CityEntity getDestinationCity() {
        return destinationCity;
    }

    public RideEntity setDestinationCity(CityEntity destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public boolean isPetFriendly() {
        return petFriendly;
    }

    public RideEntity setPetFriendly(boolean petFriendly) {
        this.petFriendly = petFriendly;
        return this;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public RideEntity setStatus(StatusEnum status) {
        this.status = status;
        return this;
    }

    public Timestamp getDepartureTime() {
        return departureTime;
    }

    public RideEntity setDepartureTime(Timestamp departureTime) {
        this.departureTime = departureTime;
        return this;
    }

    @Override
    public String toString() {
        return "RideEntity{" +
                "startCity=" + startCity +
                ", destinationCity=" + destinationCity +
                ", departureTime=" + departureTime +
                ", driver=" + driver +
                ", id=" + id +
                ", status=" + status +
                ", price=" + price +
                ", petFriendly=" + petFriendly +
                '}';
    }
}
