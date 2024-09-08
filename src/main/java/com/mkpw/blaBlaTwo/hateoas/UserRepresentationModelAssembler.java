package com.mkpw.blaBlaTwo.hateoas;

import com.mkpw.blaBlaTwo.controllers.RidesController;
import com.mkpw.blaBlaTwo.controllers.UserController;
import com.mkpw.blaBlaTwo.entity.UserEntity;
import com.mkpw.blaBlaTwo.model.User;
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
public class UserRepresentationModelAssembler extends RepresentationModelAssemblerSupport <UserEntity, User> {

    /**
     * Creates a new {@link RepresentationModelAssemblerSupport} using the given controller class and
     * resource type.
     */
    public UserRepresentationModelAssembler() {
        super(UserController.class, User.class);
    }

    /**
     * Coverts the User entity to resource
     *
     * @param entity
     */
    @Override
    public User toModel(UserEntity entity) {
        String userId = Objects.nonNull(entity.getId()) ? entity.getId().toString() : null;
        User resource = createModelWithId(entity.getId(), entity);
        BeanUtils.copyProperties(entity, resource);
        resource.add(linkTo(methodOn(RidesController.class).getRideById(userId)).withSelfRel());
        return resource;
    }

    /**
     * Coverts the collection of User entities to list of resources.
     *
     * @param entities
     */
    public List<User> toListModel(Iterable<UserEntity> entities) {
        if (Objects.isNull(entities)) {
            return List.of();
        }
        return StreamSupport.stream(entities.spliterator(), false).map(this::toModel)
                .collect(toList());
    }
}
