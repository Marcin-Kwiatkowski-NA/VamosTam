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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CityAdminServiceTest {

    private static final Long ID_100 = 100L;
    private static final Long NON_EXISTENT_ID = 999L;
    private static final String CITY_NAME = "Kraków";
    private static final String CITY_NAME_NORMALIZED = "krakow";
    private static final String ANOTHER_CITY_NAME = "Warszawa";
    private static final String ANOTHER_CITY_NAME_NORMALIZED = "warszawa";
    private static final Long PLACE_ID_1 = 3094802L;
    private static final Long PLACE_ID_2 = 756135L;
    private static final String COUNTRY_CODE = "PL";
    private static final Long POPULATION = 800000L;


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
        city = City.builder()
                .id(ID_100)
                .placeId(PLACE_ID_1)
                .namePl(CITY_NAME)
                .normNamePl(CITY_NAME_NORMALIZED)
                .countryCode(COUNTRY_CODE)
                .population(POPULATION)
                .build();

        cityDto = new CityDto(PLACE_ID_1, CITY_NAME, COUNTRY_CODE, POPULATION);
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
        when(cityRepository.findByPlaceId(PLACE_ID_1)).thenReturn(Optional.of(city));

        Optional<City> result = cityAdminService.findByPlaceId(PLACE_ID_1);

        assertTrue(result.isPresent());
        assertEquals(city, result.get());
        verify(cityRepository).findByPlaceId(PLACE_ID_1);
    }

    @Test
    @DisplayName("Return empty when finding by non-existent placeId")
    void findByPlaceIdReturnsEmptyForNonExistentPlaceId() {
        when(cityRepository.findByPlaceId(PLACE_ID_2)).thenReturn(Optional.empty());

        Optional<City> result = cityAdminService.findByPlaceId(PLACE_ID_2);

        assertFalse(result.isPresent());
        verify(cityRepository).findByPlaceId(PLACE_ID_2);
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
        CityDto cityResponseDto = new CityDto(PLACE_ID_1, CITY_NAME, COUNTRY_CODE, POPULATION);

        when(cityRepository.findByPlaceId(PLACE_ID_1)).thenReturn(Optional.empty());
        when(cityNameNormalizer.normalize(CITY_NAME)).thenReturn(CITY_NAME_NORMALIZED);
        when(cityRepository.save(any(City.class))).thenReturn(city);
        when(cityMapper.cityEntityToCityDto(city)).thenReturn(cityResponseDto);

        CityDto result = cityAdminService.create(cityDto);

        assertNotNull(result);
        assertEquals(cityResponseDto, result);
        verify(cityRepository).findByPlaceId(PLACE_ID_1);
        verify(cityNameNormalizer).normalize(CITY_NAME);
        verify(cityRepository).save(any(City.class));
        verify(cityMapper).cityEntityToCityDto(city);
    }

    @Test
    @DisplayName("Throw EntityExistsException when creating city with existing placeId")
    void createThrowsExceptionForExistingPlaceId() {
        when(cityRepository.findByPlaceId(PLACE_ID_1)).thenReturn(Optional.of(city));

        assertThrows(EntityExistsException.class,
                () -> cityAdminService.create(cityDto));
        verify(cityRepository).findByPlaceId(PLACE_ID_1);
        verify(cityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update existing city successfully")
    void updateExistingCitySuccessfully() {
        City updatedCity = City.builder()
                .id(ID_100)
                .placeId(PLACE_ID_2)
                .namePl(ANOTHER_CITY_NAME)
                .normNamePl(ANOTHER_CITY_NAME_NORMALIZED)
                .countryCode(COUNTRY_CODE)
                .population(POPULATION)
                .build();
        CityDto updateDto = new CityDto(PLACE_ID_2, ANOTHER_CITY_NAME, COUNTRY_CODE, POPULATION);
        CityDto updatedResponseDto = new CityDto(PLACE_ID_2, ANOTHER_CITY_NAME, COUNTRY_CODE, POPULATION);

        when(cityRepository.findById(ID_100)).thenReturn(Optional.of(city));
        when(cityRepository.findByPlaceId(PLACE_ID_2)).thenReturn(Optional.empty());
        when(cityNameNormalizer.normalize(ANOTHER_CITY_NAME)).thenReturn(ANOTHER_CITY_NAME_NORMALIZED);
        when(cityRepository.save(any(City.class))).thenReturn(updatedCity);
        when(cityMapper.cityEntityToCityDto(updatedCity)).thenReturn(updatedResponseDto);

        CityDto result = cityAdminService.update(updateDto, ID_100);

        assertNotNull(result);
        assertEquals(updatedResponseDto, result);
        verify(cityRepository).findById(ID_100);
        verify(cityRepository).findByPlaceId(PLACE_ID_2);
        verify(cityNameNormalizer).normalize(ANOTHER_CITY_NAME);
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
        City existingOtherCity = City.builder()
                .id(200L)
                .placeId(PLACE_ID_2)
                .namePl(ANOTHER_CITY_NAME)
                .normNamePl(ANOTHER_CITY_NAME_NORMALIZED)
                .countryCode(COUNTRY_CODE)
                .population(POPULATION)
                .build();
        CityDto updateDto = new CityDto(PLACE_ID_2, ANOTHER_CITY_NAME, COUNTRY_CODE, POPULATION);

        when(cityRepository.findById(ID_100)).thenReturn(Optional.of(city));
        when(cityRepository.findByPlaceId(PLACE_ID_2)).thenReturn(Optional.of(existingOtherCity));

        assertThrows(EntityExistsException.class,
                () -> cityAdminService.update(updateDto, ID_100));
        verify(cityRepository).findById(ID_100);
        verify(cityRepository).findByPlaceId(PLACE_ID_2);
        verify(cityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update city with same placeId succeeds (no conflict)")
    void updateCityWithSamePlaceIdSucceeds() {
        CityDto updateDto = new CityDto(PLACE_ID_1, ANOTHER_CITY_NAME, COUNTRY_CODE, POPULATION);
        City updatedCity = City.builder()
                .id(ID_100)
                .placeId(PLACE_ID_1)
                .namePl(ANOTHER_CITY_NAME)
                .normNamePl(ANOTHER_CITY_NAME_NORMALIZED)
                .countryCode(COUNTRY_CODE)
                .population(POPULATION)
                .build();
        CityDto updatedResponseDto = new CityDto(PLACE_ID_1, ANOTHER_CITY_NAME, COUNTRY_CODE, POPULATION);

        when(cityRepository.findById(ID_100)).thenReturn(Optional.of(city));
        when(cityRepository.findByPlaceId(PLACE_ID_1)).thenReturn(Optional.of(city));
        when(cityNameNormalizer.normalize(ANOTHER_CITY_NAME)).thenReturn(ANOTHER_CITY_NAME_NORMALIZED);
        when(cityRepository.save(any(City.class))).thenReturn(updatedCity);
        when(cityMapper.cityEntityToCityDto(updatedCity)).thenReturn(updatedResponseDto);

        CityDto result = cityAdminService.update(updateDto, ID_100);

        assertNotNull(result);
        assertEquals(updatedResponseDto, result);
        verify(cityRepository).findById(ID_100);
        verify(cityRepository).findByPlaceId(PLACE_ID_1);
        verify(cityNameNormalizer).normalize(ANOTHER_CITY_NAME);
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
