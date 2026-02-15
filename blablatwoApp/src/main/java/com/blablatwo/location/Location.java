package com.blablatwo.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "location", indexes = {
        @Index(name = "idx_location_osm_id", columnList = "osmId", unique = true)
})
public class Location {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private Long osmId;

    @Column(nullable = false)
    private String name;

    private String country;

    @Column(length = 2)
    private String countryCode;

    private String state;
    private String county;
    private String city;
    private String postCode;

    @Column(nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point coordinates;

    private String type;
    private String osmKey;
    private String osmValue;

    public Double getLatitude() {
        return coordinates != null ? coordinates.getY() : null;
    }

    public Double getLongitude() {
        return coordinates != null ? coordinates.getX() : null;
    }

    @Override
    public String toString() {
        return "Location{id=%d, osmId=%d, name='%s'}".formatted(id, osmId, name);
    }
}
