package com.mkpw.blaBlaTwo.hateoas;

import com.mkpw.blaBlaTwo.controllers.CityController;
import com.mkpw.blaBlaTwo.controllers.RidesController;
import com.mkpw.blaBlaTwo.entity.CityEntity;
import com.mkpw.blaBlaTwo.model.City;
import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class CityRepresentationModelAssembler extends RepresentationModelAssemblerSupport <CityEntity, City>  {

    /**
     * Creates a new {@link RepresentationModelAssemblerSupport} using the given controller class and
     * resource type.
     */
    public CityRepresentationModelAssembler() {
        super(CityController.class, City.class);
    }

    /**
     * Coverts the City entity to resource
     *
     * @param entity
     */
    @Override
    public City toModel(CityEntity entity) {
        String cityId = Objects.nonNull(entity.getId()) ? entity.getId().toString() : null;
        City resource = createModelWithId(entity.getId(), entity);
        BeanUtils.copyProperties(entity, resource);
        resource.add(linkTo(methodOn(RidesController.class).getRideById(cityId)).withSelfRel());
        return resource;
    }

    /**
     * Coverts the collection of City entities to list of resources.
     *
     * @param entities
     */
    public List<City> toListModel(Iterable<CityEntity> entities) {
        if (Objects.isNull(entities)) {
            return List.of();
        }
        return StreamSupport.stream(entities.spliterator(), false).map(this::toModel)
                .collect(toList());
    }
}
