package com.blablatwo.city;

import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Admin service for city CRUD operations.
 * Used by admin/CRUD controllers for managing city records.
 */
@Service
public class CityAdminService {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;
    private final CityNameNormalizer cityNameNormalizer;

    public CityAdminService(CityRepository cityRepository, CityMapper cityMapper, CityNameNormalizer cityNameNormalizer) {
        this.cityRepository = cityRepository;
        this.cityMapper = cityMapper;
        this.cityNameNormalizer = cityNameNormalizer;
    }

    @Transactional
    public Optional<City> getById(Long id) {
        return cityRepository.findById(id);
    }

    @Transactional
    public Optional<City> findByPlaceId(Long placeId) {
        return cityRepository.findByPlaceId(placeId);
    }

    @Transactional
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    @Transactional
    public CityDto create(CityDto cityDto) {
        if (cityRepository.findByPlaceId(cityDto.placeId()).isPresent()) {
            throw new EntityExistsException("City with placeId '" + cityDto.placeId() + "' already exists.");
        }
        String normalizedName = cityNameNormalizer.normalize(cityDto.name());
        City newCity = City.builder()
                .placeId(cityDto.placeId())
                .namePl(cityDto.name())
                .normNamePl(normalizedName)
                .countryCode(cityDto.countryCode())
                .population(cityDto.population())
                .build();
        return cityMapper.cityEntityToCityDto(cityRepository.save(newCity));
    }

    @Transactional
    public CityDto update(CityDto cityDto, Long id) {
        City existingCity = cityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("City with ID " + id + " not found."));

        Optional<City> conflictingCity = cityRepository.findByPlaceId(cityDto.placeId());
        if (conflictingCity.isPresent() && !conflictingCity.get().getId().equals(id)) {
            throw new EntityExistsException("City with placeId '" + cityDto.placeId() + "' already exists for another ID.");
        }

        String normalizedName = cityNameNormalizer.normalize(cityDto.name());
        existingCity.setPlaceId(cityDto.placeId());
        existingCity.setNamePl(cityDto.name());
        existingCity.setNormNamePl(normalizedName);
        existingCity.setCountryCode(cityDto.countryCode());
        existingCity.setPopulation(cityDto.population());

        return cityMapper.cityEntityToCityDto(cityRepository.save(existingCity));
    }

    @Transactional
    public void delete(Long id) {
        if (!cityRepository.existsById(id)) {
            throw new NoSuchElementException("City with ID " + id + " not found.");
        }
        cityRepository.deleteById(id);
    }
}
