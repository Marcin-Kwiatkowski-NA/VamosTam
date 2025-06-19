package com.blablatwo.city;

import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;

    @Autowired
    public CityServiceImpl(CityRepository cityRepository, CityMapper cityMapper) {
        this.cityRepository = cityRepository;
        this.cityMapper = cityMapper;
    }

    @Override
    @Transactional
    public Optional<City> getById(Long id) {
        return cityRepository.findById(id);
    }

    @Override
    @Transactional
    public Optional<City> findByName(String name) {
        return cityRepository.findByNameIgnoreCase(name);
    }

    @Override
    @Transactional
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    @Override
    @Transactional
    public CityDto create(CityDto cityDto) {
        if (cityRepository.existsByName(cityDto.name())) {
            throw new EntityExistsException("City with name '" + cityDto.name() + "' already exists.");
        }
        City newCity = cityMapper.cityDtoToEntity(cityDto);
        return cityMapper.cityEntityToCityDto(cityRepository.save(newCity));
    }

    @Override
    @Transactional
    public CityDto update(CityDto cityDto, Long id) {
        City existingCity = cityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("City with ID " + id + " not found."));

        if (cityRepository.findByNameIgnoreCase(cityDto.name())
                .filter(city -> !city.getId().equals(id))
                .isPresent()) {
            throw new EntityExistsException("City with name '" + cityDto.name() + "' already exists for another ID.");
        }

        cityMapper.update(existingCity, cityDto);
        return cityMapper.cityEntityToCityDto(cityRepository.save(existingCity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!cityRepository.existsById(id)) {
            throw new NoSuchElementException("City with ID " + id + " not found.");
        }
        cityRepository.deleteById(id);
    }
}