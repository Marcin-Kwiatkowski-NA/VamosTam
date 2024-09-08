package com.mkpw.blaBlaTwo.repository;

import com.mkpw.blaBlaTwo.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface UserRepository extends CrudRepository<UserEntity, UUID> {
}
