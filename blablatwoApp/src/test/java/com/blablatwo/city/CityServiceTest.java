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
class CityServiceTest {

    private static final Long ID_100 = 100L;
    private static final Long NON_EXISTENT_ID = 999L;
    private static final String CITY_NAME = "Krakow";
    private static final String ANOTHER_CITY_NAME = "Warsaw";

    @Mock
    private CityRepository cityRepository;

    @Mock
    private CityMapper cityMapper;

    @InjectMocks
    private CityServiceImpl cityService;

    private City city;
    private CityCreationDto cityCreationDto;
    private CityResponseDto cityResponseDto;

    @BeforeEach
    void setUp() {
        city = City.builder()
                .id(ID_100)
                .name(CITY_NAME)
                .build();

        cityCreationDto = new CityCreationDto(CITY_NAME);
        cityResponseDto = new CityResponseDto(ID_100, CITY_NAME);
    }

    @Test
    @DisplayName("Return city when finding by existing ID")
    void getByIdReturnsCity() {
        when(cityRepository.findById(ID_100)).thenReturn(Optional.of(city));

        Optional<City> result = cityService.getById(ID_100);

        assertTrue(result.isPresent());
        assertEquals(city, result.get());
        verify(cityRepository).findById(ID_100);
        verify(cityMapper, never()).cityEntityToCityResponseDto(any());
    }

    @Test
    @DisplayName("Return empty when finding by non-existent ID")
    void getByIdReturnsEmptyForNonExistentId() {
        when(cityRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        Optional<City> result = cityService.getById(NON_EXISTENT_ID);

        assertFalse(result.isPresent());
        verify(cityRepository).findById(NON_EXISTENT_ID);
        verify(cityMapper, never()).cityEntityToCityResponseDto(any());
    }

    @Test
    @DisplayName("Return city when finding by existing name")
    void findByNameReturnsCity() {
        when(cityRepository.findByName(CITY_NAME)).thenReturn(Optional.of(city));

        Optional<City> result = cityService.findByName(CITY_NAME);

        assertTrue(result.isPresent());
        assertEquals(city, result.get());
        verify(cityRepository).findByName(CITY_NAME);
        verify(cityMapper, never()).cityEntityToCityResponseDto(any());
    }

    @Test
    @DisplayName("Return empty when finding by non-existent name")
    void findByNameReturnsEmptyForNonExistentName() {
        when(cityRepository.findByName(ANOTHER_CITY_NAME)).thenReturn(Optional.empty());

        Optional<City> result = cityService.findByName(ANOTHER_CITY_NAME);

        assertFalse(result.isPresent());
        verify(cityRepository).findByName(ANOTHER_CITY_NAME);
        verify(cityMapper, never()).cityEntityToCityResponseDto(any());
    }

    @Test
    @DisplayName("Return all cities successfully")
    void getAllCitiesSuccessfully() {
        when(cityRepository.findAll()).thenReturn(List.of(city));

        List<City> result = cityService.getAllCities();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(city, result.get(0));
        verify(cityRepository).findAll();
        verify(cityMapper, never()).cityEntityToCityResponseDto(any());
    }

    @Test
    @DisplayName("Create new city successfully")
    void createNewCitySuccessfully() {
        when(cityRepository.existsByName(CITY_NAME)).thenReturn(false);
        when(cityMapper.cityCreationDtoToEntity(cityCreationDto)).thenReturn(city);
        when(cityRepository.save(city)).thenReturn(city);
        when(cityMapper.cityEntityToCityResponseDto(city)).thenReturn(cityResponseDto);

        CityResponseDto result = cityService.create(cityCreationDto);

        assertNotNull(result);
        assertEquals(cityResponseDto, result);
        verify(cityRepository).existsByName(CITY_NAME);
        verify(cityMapper).cityCreationDtoToEntity(cityCreationDto);
        verify(cityRepository).save(city);
        verify(cityMapper).cityEntityToCityResponseDto(city);
    }

    @Test
    @DisplayName("Throw EntityExistsException when creating city with existing name")
    void createThrowsExceptionForExistingName() {
        when(cityRepository.existsByName(CITY_NAME)).thenReturn(true);

        assertThrows(EntityExistsException.class,
                () -> cityService.create(cityCreationDto));
        verify(cityRepository).existsByName(CITY_NAME);
        verify(cityMapper, never()).cityCreationDtoToEntity(any());
        verify(cityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update existing city successfully")
    void updateExistingCitySuccessfully() {
        City updatedCity = City.builder().id(ID_100).name(ANOTHER_CITY_NAME).build();
        CityCreationDto updateDto = new CityCreationDto(ANOTHER_CITY_NAME);
        CityResponseDto updatedResponseDto = new CityResponseDto(ID_100, ANOTHER_CITY_NAME);

        when(cityRepository.findById(ID_100)).thenReturn(Optional.of(city));
        when(cityRepository.findByName(ANOTHER_CITY_NAME)).thenReturn(Optional.empty());
        doNothing().when(cityMapper).update(city, updateDto);
        when(cityRepository.save(city)).thenReturn(updatedCity);
        when(cityMapper.cityEntityToCityResponseDto(updatedCity)).thenReturn(updatedResponseDto);

        CityResponseDto result = cityService.update(updateDto, ID_100);

        assertNotNull(result);
        assertEquals(updatedResponseDto, result);
        verify(cityRepository).findById(ID_100);
        verify(cityRepository).findByName(ANOTHER_CITY_NAME);
        verify(cityMapper).update(city, updateDto);
        verify(cityRepository).save(city);
        verify(cityMapper).cityEntityToCityResponseDto(updatedCity);
    }

    @Test
    @DisplayName("Throw NoSuchElementException when updating non-existent city")
    void throwExceptionWhenUpdatingNonExistentCity() {
        when(cityRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> cityService.update(cityCreationDto, NON_EXISTENT_ID));
        verify(cityRepository).findById(NON_EXISTENT_ID);
        verify(cityMapper, never()).update(any(), any());
        verify(cityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throw EntityExistsException when updating city with name that clashes with another city")
    void updateThrowsExceptionWhenNameClashes() {
        City existingOtherCity = City.builder().id(200L).name(ANOTHER_CITY_NAME).build();
        CityCreationDto updateDto = new CityCreationDto(ANOTHER_CITY_NAME);

        when(cityRepository.findById(ID_100)).thenReturn(Optional.of(city));
        when(cityRepository.findByName(ANOTHER_CITY_NAME)).thenReturn(Optional.of(existingOtherCity));

        assertThrows(EntityExistsException.class,
                () -> cityService.update(updateDto, ID_100));
        verify(cityRepository).findById(ID_100);
        verify(cityRepository).findByName(ANOTHER_CITY_NAME);
        verify(cityMapper, never()).update(any(), any());
        verify(cityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete existing city successfully")
    void deleteExistingCitySuccessfully() {
        when(cityRepository.existsById(ID_100)).thenReturn(true);
        doNothing().when(cityRepository).deleteById(ID_100);

        cityService.delete(ID_100);

        verify(cityRepository).existsById(ID_100);
        verify(cityRepository).deleteById(ID_100);
    }

    @Test
    @DisplayName("Throw NoSuchElementException when deleting non-existent city")
    void throwExceptionWhenDeletingNonExistentCity() {
        when(cityRepository.existsById(NON_EXISTENT_ID)).thenReturn(false);

        assertThrows(NoSuchElementException.class,
                () -> cityService.delete(NON_EXISTENT_ID));
        verify(cityRepository).existsById(NON_EXISTENT_ID);
        verify(cityRepository, never()).deleteById(anyLong());
    }
}