package com.mkpw.blaBlaTwo.hateoas;

import com.mkpw.blaBlaTwo.controllers.RidesController;
import com.mkpw.blaBlaTwo.entity.RideEntity;
import com.mkpw.blaBlaTwo.model.Ride;
import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class RideRepresentationModelAssembler extends RepresentationModelAssemblerSupport<RideEntity, Ride> {

    private final CityRepresentationModelAssembler cityAssembler;
    private final UserRepresentationModelAssembler userAssembler;

    /**
     * Creates a new {@link RepresentationModelAssemblerSupport} using the given controller class and
     * resource type.
     */
    public RideRepresentationModelAssembler(CityRepresentationModelAssembler cityAssembler, UserRepresentationModelAssembler userAssembler) {
        super(RidesController.class, Ride.class);
        this.cityAssembler = cityAssembler;
        this.userAssembler = userAssembler;
    }

    /**
     * Coverts the Ride entity to resource
     *
     * @param entity
     */
    @Override
    public Ride toModel(RideEntity entity) {
        String rideId = Objects.nonNull(entity.getId()) ? entity.getId().toString() : null;
        Ride resource = new Ride();
        BeanUtils.copyProperties(entity, resource);
        resource.departureTime(entity.getDepartureTime().toInstant().atOffset(ZoneOffset.UTC))
                .driver(userAssembler.toModel(entity.getDriver()))
                .startCity(cityAssembler.toModel(entity.getStartCity()))
                .destinationCity(cityAssembler.toModel(entity.getDestinationCity()));
        resource.add(linkTo(methodOn(RidesController.class).getRideById(rideId)).withSelfRel());
        return resource;
    }

    /**
     * Coverts the collection of Ride entities to list of resources.
     *
     * @param entities
     */
    public List<Ride> toListModel(Iterable<RideEntity> entities) {
        if (Objects.isNull(entities)) {
            return List.of();
        }
        return StreamSupport.stream(entities.spliterator(), false).map(this::toModel)
                .collect(toList());
    }
}
