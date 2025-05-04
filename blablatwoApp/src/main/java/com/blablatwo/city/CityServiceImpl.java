package com.blablatwo.city;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class CityServiceImpl implements CityService{

    private static final Logger LOGGER = LoggerFactory.getLogger(CityServiceImpl.class);
    CityRepositoryJdbc repository;
    CityMapper mapper;

    public CityServiceImpl(CityRepositoryJdbc repository, CityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public CityDTO getById(Long id) {
        return mapper.toCityDTO(
                repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("City not found with ID " + id)));
    }

    @Override
    public Collection<CityDTO> getAllCities() {
        Collection<CityDTO> list = new ArrayList<>();
        for (var city : repository.findAll()) {
            list.add(mapper.toCityDTO(city));
        }
        return list;
    }
}
