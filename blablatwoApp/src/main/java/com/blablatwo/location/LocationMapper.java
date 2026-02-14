package com.blablatwo.location;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Mapping(target = "latitude", source = "location", qualifiedByName = "toLatitude")
    @Mapping(target = "longitude", source = "location", qualifiedByName = "toLongitude")
    LocationDto locationToDto(Location location);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coordinates", source = "ref", qualifiedByName = "toPoint")
    Location locationRefToEntity(LocationRef ref);

    @Named("toLatitude")
    default Double toLatitude(Location location) {
        return location.getLatitude();
    }

    @Named("toLongitude")
    default Double toLongitude(Location location) {
        return location.getLongitude();
    }

    @Named("toPoint")
    default Point toPoint(LocationRef ref) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(ref.longitude(), ref.latitude()));
    }
}
