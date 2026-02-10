package com.blablatwo.city;

import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.blablatwo.util.Constants.*;
import static com.blablatwo.util.TestFixtures.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CityAdminServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private CityMapper cityMapper;

    @Mock
    private CityNameNormalizer cityNameNormalizer;

    @InjectMocks
    private CityAdminService cityAdminService;

    private City city;
    private CityDto cityDto;

    @BeforeEach
    void setUp() {
        city = aKrakowCity().id(ID_100).build();
        cityDto = krakowCityDto();
    }

    @Test
    @DisplayName("Return city when finding by existing ID")
    void getByIdReturnsCity() {
        when(cityRepository.findById(ID_100)).thenReturn(Optional.of(city));

        Optional<City> result = cityAdminService.getById(ID_100);

        assertTrue(result.isPresent());
        assertEquals(city, result.get());
        verify(cityRepository).findById(ID_100);
    }

    @Test
    @DisplayName("Return empty when finding by non-existent ID")
    void getByIdReturnsEmptyForNonExistentId() {
        when(cityRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        Optional<City> result = cityAdminService.getById(NON_EXISTENT_ID);

        assertFalse(result.isPresent());
        verify(cityRepository).findById(NON_EXISTENT_ID);
    }

    @Test
    @DisplayName("Return city when finding by existing placeId")
    void findByPlaceIdReturnsCity() {
        when(cityRepository.findByPlaceId(PLACE_ID_KRAKOW)).thenReturn(Optional.of(city));

        Optional<City> result = cityAdminService.findByPlaceId(PLACE_ID_KRAKOW);

        assertTrue(result.isPresent());
        assertEquals(city, result.get());
        verify(cityRepository).findByPlaceId(PLACE_ID_KRAKOW);
    }

    @Test
    @DisplayName("Return empty when finding by non-existent placeId")
    void findByPlaceIdReturnsEmptyForNonExistentPlaceId() {
        when(cityRepository.findByPlaceId(PLACE_ID_WARSAW)).thenReturn(Optional.empty());

        Optional<City> result = cityAdminService.findByPlaceId(PLACE_ID_WARSAW);

        assertFalse(result.isPresent());
        verify(cityRepository).findByPlaceId(PLACE_ID_WARSAW);
    }

    @Test
    @DisplayName("Return all cities successfully")
    void getAllCitiesSuccessfully() {
        when(cityRepository.findAll()).thenReturn(List.of(city));

        List<City> result = cityAdminService.getAllCities();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(city, result.get(0));
        verify(cityRepository).findAll();
    }

    @Test
    @DisplayName("Create new city successfully")
    void createNewCitySuccessfully() {
        CityDto cityResponseDto = krakowCityDto();

        when(cityRepository.findByPlaceId(PLACE_ID_KRAKOW)).thenReturn(Optional.empty());
        when(cityNameNormalizer.normalize(CITY_NAME_KRAKOW)).thenReturn(CITY_NAME_KRAKOW.toLowerCase());
        when(cityRepository.save(any(City.class))).thenReturn(city);
        when(cityMapper.cityEntityToCityDto(city)).thenReturn(cityResponseDto);

        CityDto result = cityAdminService.create(cityDto);

        assertNotNull(result);
        assertEquals(cityResponseDto, result);
        verify(cityRepository).findByPlaceId(PLACE_ID_KRAKOW);
        verify(cityNameNormalizer).normalize(CITY_NAME_KRAKOW);
        verify(cityRepository).save(any(City.class));
        verify(cityMapper).cityEntityToCityDto(city);
    }

    @Test
    @DisplayName("Throw EntityExistsException when creating city with existing placeId")
    void createThrowsExceptionForExistingPlaceId() {
        when(cityRepository.findByPlaceId(PLACE_ID_KRAKOW)).thenReturn(Optional.of(city));

        assertThrows(EntityExistsException.class,
                () -> cityAdminService.create(cityDto));
        verify(cityRepository).findByPlaceId(PLACE_ID_KRAKOW);
        verify(cityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update existing city successfully")
    void updateExistingCitySuccessfully() {
        City updatedCity = aWarsawCity().id(ID_100).build();
        CityDto updateDto = warsawCityDto();
        CityDto updatedResponseDto = warsawCityDto();

        when(cityRepository.findById(ID_100)).thenReturn(Optional.of(city));
        when(cityRepository.findByPlaceId(PLACE_ID_WARSAW)).thenReturn(Optional.empty());
        when(cityNameNormalizer.normalize(CITY_NAME_WARSAW)).thenReturn(CITY_NAME_WARSAW.toLowerCase());
        when(cityRepository.save(any(City.class))).thenReturn(updatedCity);
        when(cityMapper.cityEntityToCityDto(updatedCity)).thenReturn(updatedResponseDto);

        CityDto result = cityAdminService.update(updateDto, ID_100);

        assertNotNull(result);
        assertEquals(updatedResponseDto, result);
        verify(cityRepository).findById(ID_100);
        verify(cityRepository).findByPlaceId(PLACE_ID_WARSAW);
        verify(cityNameNormalizer).normalize(CITY_NAME_WARSAW);
        verify(cityRepository).save(any(City.class));
        verify(cityMapper).cityEntityToCityDto(updatedCity);
    }

    @Test
    @DisplayName("Throw NoSuchElementException when updating non-existent city")
    void throwExceptionWhenUpdatingNonExistentCity() {
        when(cityRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> cityAdminService.update(cityDto, NON_EXISTENT_ID));
        verify(cityRepository).findById(NON_EXISTENT_ID);
        verify(cityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throw EntityExistsException when updating city with placeId that clashes with another city")
    void updateThrowsExceptionWhenPlaceIdClashes() {
        City existingOtherCity = aWarsawCity().id(200L).build();
        CityDto updateDto = warsawCityDto();

        when(cityRepository.findById(ID_100)).thenReturn(Optional.of(city));
        when(cityRepository.findByPlaceId(PLACE_ID_WARSAW)).thenReturn(Optional.of(existingOtherCity));

        assertThrows(EntityExistsException.class,
                () -> cityAdminService.update(updateDto, ID_100));
        verify(cityRepository).findById(ID_100);
        verify(cityRepository).findByPlaceId(PLACE_ID_WARSAW);
        verify(cityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update city with same placeId succeeds (no conflict)")
    void updateCityWithSamePlaceIdSucceeds() {
        CityDto updateDto = new CityDto(PLACE_ID_KRAKOW, CITY_NAME_WARSAW, "PL", POPULATION_KRAKOW);
        City updatedCity = aKrakowCity()
                .id(ID_100)
                .namePl(CITY_NAME_WARSAW)
                .normNamePl(CITY_NAME_WARSAW.toLowerCase())
                .build();
        CityDto updatedResponseDto = new CityDto(PLACE_ID_KRAKOW, CITY_NAME_WARSAW, "PL", POPULATION_KRAKOW);

        when(cityRepository.findById(ID_100)).thenReturn(Optional.of(city));
        when(cityRepository.findByPlaceId(PLACE_ID_KRAKOW)).thenReturn(Optional.of(city));
        when(cityNameNormalizer.normalize(CITY_NAME_WARSAW)).thenReturn(CITY_NAME_WARSAW.toLowerCase());
        when(cityRepository.save(any(City.class))).thenReturn(updatedCity);
        when(cityMapper.cityEntityToCityDto(updatedCity)).thenReturn(updatedResponseDto);

        CityDto result = cityAdminService.update(updateDto, ID_100);

        assertNotNull(result);
        assertEquals(updatedResponseDto, result);
        verify(cityRepository).findById(ID_100);
        verify(cityRepository).findByPlaceId(PLACE_ID_KRAKOW);
        verify(cityNameNormalizer).normalize(CITY_NAME_WARSAW);
        verify(cityRepository).save(any(City.class));
    }

    @Test
    @DisplayName("Delete existing city successfully")
    void deleteExistingCitySuccessfully() {
        when(cityRepository.existsById(ID_100)).thenReturn(true);
        doNothing().when(cityRepository).deleteById(ID_100);

        cityAdminService.delete(ID_100);

        verify(cityRepository).existsById(ID_100);
        verify(cityRepository).deleteById(ID_100);
    }

    @Test
    @DisplayName("Throw NoSuchElementException when deleting non-existent city")
    void throwExceptionWhenDeletingNonExistentCity() {
        when(cityRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

        assertThrows(NoSuchElementException.class,
                () -> cityAdminService.delete(NON_EXISTENT_ID));
        verify(cityRepository).existsById(NON_EXISTENT_ID);
        verify(cityRepository, never()).deleteById(anyLong());
    }
}
