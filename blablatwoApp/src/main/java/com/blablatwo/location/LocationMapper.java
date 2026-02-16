package com.blablatwo.location;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.context.i18n.LocaleContextHolder;

@Mapper(componentModel = "spring")
public abstract class LocationMapper {

    static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Mapping(target = "name", source = ".", qualifiedByName = "resolveName")
    @Mapping(target = "latitude", source = "location", qualifiedByName = "toLatitude")
    @Mapping(target = "longitude", source = "location", qualifiedByName = "toLongitude")
    public abstract LocationDto locationToDto(Location location);

    @Named("resolveName")
    protected String resolveName(Location location) {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return "pl".equals(lang) ? location.getNamePl() : location.getNameEn();
    }

    @Named("toLatitude")
    protected Double toLatitude(Location location) {
        return location.getLatitude();
    }

    @Named("toLongitude")
    protected Double toLongitude(Location location) {
        return location.getLongitude();
    }

    @Named("toPoint")
    protected Point toPoint(LocationRef ref) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(ref.longitude(), ref.latitude()));
    }
}
