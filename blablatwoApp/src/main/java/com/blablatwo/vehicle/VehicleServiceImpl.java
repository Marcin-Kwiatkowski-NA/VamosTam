package com.blablatwo.vehicle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Autowired
    public VehicleServiceImpl(VehicleRepository vehicleRepository, VehicleMapper vehicleMapper) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehicle> getById(Long id) {
        return vehicleRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @Override
    @Transactional
    public VehicleResponseDto create(VehicleCreationDto vehicleDto) {
        var newVehicleEntity = vehicleMapper.vehicleCreationDtoToEntity(vehicleDto);
        return vehicleMapper.vehicleEntityToVehicleResponseDto(
                vehicleRepository.save(newVehicleEntity));
    }

    @Override
    @Transactional
    public VehicleResponseDto update(VehicleCreationDto vehicleDto, Long id) {
        var existingVehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vehicle with ID " + id + " not found."));

        vehicleMapper.update(existingVehicle, vehicleDto);
        return vehicleMapper.vehicleEntityToVehicleResponseDto(
                vehicleRepository.save(existingVehicle));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (vehicleRepository.existsById(id)) {
            vehicleRepository.deleteById(id);
        } else {
            throw new NoSuchElementException("Vehicle with ID " + id + " not found.");
        }
    }
}