package com.vamigo.vehicle;

import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.exception.NoSuchUserException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final UserAccountRepository userAccountRepository;
    private final VehiclePhotoUrlResolver photoUrlResolver;

    public VehicleServiceImpl(VehicleRepository vehicleRepository,
                              VehicleMapper vehicleMapper,
                              UserAccountRepository userAccountRepository,
                              VehiclePhotoUrlResolver photoUrlResolver) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
        this.userAccountRepository = userAccountRepository;
        this.photoUrlResolver = photoUrlResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDto> getMyVehicles(Long userId) {
        return vehicleRepository.findByOwnerId(userId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public VehicleResponseDto create(Long userId, VehicleCreationDto dto) {
        UserAccount owner = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        Vehicle entity = vehicleMapper.vehicleCreationDtoToEntity(dto);
        entity.setOwner(owner);

        return toResponseDto(vehicleRepository.save(entity));
    }

    @Override
    @Transactional
    public VehicleResponseDto update(Long userId, Long vehicleId, VehicleCreationDto dto) {
        Vehicle existing = vehicleRepository.findByIdAndOwnerId(vehicleId, userId)
                .orElseThrow(() -> new NoSuchElementException("Vehicle with ID " + vehicleId + " not found."));

        vehicleMapper.update(existing, dto);
        return toResponseDto(vehicleRepository.save(existing));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long vehicleId) {
        Vehicle existing = vehicleRepository.findByIdAndOwnerId(vehicleId, userId)
                .orElseThrow(() -> new NoSuchElementException("Vehicle with ID " + vehicleId + " not found."));

        vehicleRepository.delete(existing);
    }

    private VehicleResponseDto toResponseDto(Vehicle vehicle) {
        VehicleResponseDto dto = vehicleMapper.vehicleEntityToVehicleResponseDto(vehicle);
        return dto.toBuilder()
                .photoUrl(photoUrlResolver.resolve(vehicle))
                .build();
    }

    /**
     * Build a response DTO with the license plate masked.
     * Used for public-facing contexts (ride details, user cards).
     */
    public VehicleResponseDto toMaskedResponseDto(Vehicle vehicle) {
        VehicleResponseDto dto = vehicleMapper.vehicleEntityToVehicleResponseDto(vehicle);
        return dto.toBuilder()
                .photoUrl(photoUrlResolver.resolve(vehicle))
                .licensePlate(LicensePlateMasker.mask(vehicle.getLicensePlate()))
                .build();
    }

    /**
     * Build a response DTO with the full license plate revealed.
     * Used only for confirmed passengers.
     */
    public VehicleResponseDto toFullResponseDto(Vehicle vehicle) {
        return toResponseDto(vehicle);
    }
}
