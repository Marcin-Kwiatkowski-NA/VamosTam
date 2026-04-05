package com.vamigo.user;

import com.vamigo.domain.PersonDisplayNameResolver;
import com.vamigo.domain.Status;
import com.vamigo.dto.UserCardDto;
import com.vamigo.location.Location;
import com.vamigo.location.LocationDto;
import com.vamigo.location.LocationMapper;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideMapper;
import com.vamigo.ride.RideRepository;
import com.vamigo.ride.RideResponseEnricher;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.user.dto.CarrierDirectionsDto;
import com.vamigo.user.dto.CarrierProfileDto;
import com.vamigo.user.dto.CarrierPublicPageDto;
import com.vamigo.user.exception.NoSuchUserException;
import com.vamigo.vehicle.VehicleMapper;
import com.vamigo.vehicle.VehiclePhotoUrlResolver;
import com.vamigo.vehicle.VehicleRepository;
import com.vamigo.vehicle.VehicleResponseDto;
import com.vamigo.vehicle.LicensePlateMasker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CarrierPublicPageController {

    private final CarrierProfileRepository carrierProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final VehiclePhotoUrlResolver vehiclePhotoUrlResolver;
    private final PersonDisplayNameResolver displayNameResolver;
    private final AvatarUrlResolver avatarUrlResolver;
    private final RideRepository rideRepository;
    private final RideMapper rideMapper;
    private final RideResponseEnricher rideResponseEnricher;
    private final LocationMapper locationMapper;

    @GetMapping("/carriers/{slug}")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<CarrierPublicPageDto> getCarrierPublicPage(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        CarrierProfile carrier = carrierProfileRepository.findBySlug(slug.toLowerCase())
                .orElseThrow(() -> new NoSuchUserException("No carrier found with slug: " + slug));

        Long userId = carrier.getId();

        CarrierProfileDto carrierDto = toCarrierDto(carrier);
        UserCardDto userCard = buildUserCard(userId);

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<Ride> ridePage = rideRepository.findByDriverIdAndStatusAndDepartureTimeAfterOrderByDepartureTimeAsc(
                userId, Status.ACTIVE, Instant.now(), pageable);

        List<Ride> rides = ridePage.getContent();
        List<RideResponseDto> dtos = rides.stream()
                .map(rideMapper::rideEntityToRideResponseDto)
                .toList();
        List<RideResponseDto> enrichedRides = rideResponseEnricher.enrich(rides, dtos);

        CarrierPublicPageDto response = new CarrierPublicPageDto(
                carrierDto,
                userCard,
                enrichedRides,
                (int) ridePage.getTotalElements(),
                ridePage.getTotalPages(),
                ridePage.getNumber()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/carriers/{slug}/directions")
    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public ResponseEntity<CarrierDirectionsDto> getCarrierDirections(@PathVariable String slug) {
        CarrierProfile carrier = carrierProfileRepository.findBySlug(slug.toLowerCase())
                .orElseThrow(() -> new NoSuchUserException("No carrier found with slug: " + slug));

        List<Location> locations = rideRepository.findDistinctLocationsByDriverId(
                carrier.getId(), Instant.now());

        List<LocationDto> locationDtos = locations.stream()
                .map(locationMapper::locationToDto)
                .toList();

        return ResponseEntity.ok(new CarrierDirectionsDto(locationDtos));
    }

    private CarrierProfileDto toCarrierDto(CarrierProfile carrier) {
        return new CarrierProfileDto(
                carrier.getId(),
                carrier.getCompanyName(),
                carrier.getNip(),
                carrier.getWebsiteUrl(),
                carrier.isBookingEnabled(),
                carrier.getSlug(),
                carrier.getDescription()
        );
    }

    private UserCardDto buildUserCard(Long userId) {
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        UserAccount account = userAccountRepository.findById(userId).orElse(null);
        List<VehicleResponseDto> vehicles = vehicleRepository.findByOwnerId(userId).stream()
                .map(v -> {
                    VehicleResponseDto dto = vehicleMapper.vehicleEntityToVehicleResponseDto(v);
                    return dto.toBuilder()
                            .photoUrl(vehiclePhotoUrlResolver.resolve(v))
                            .licensePlate(LicensePlateMasker.mask(v.getLicensePlate()))
                            .build();
                })
                .toList();

        String name = displayNameResolver.resolveInternal(profile, userId);
        String avatarUrl = avatarUrlResolver.resolve(profile);
        String bio = null;
        boolean emailVerified = false;
        boolean phoneVerified = false;
        int ridesGiven = 0;
        int ridesTaken = 0;
        int ratingCount = 0;
        Double rating = null;

        if (profile != null) {
            bio = profile.getBio();
            UserStats stats = profile.getStats();
            if (stats != null) {
                ridesGiven = stats.getRidesGiven();
                ridesTaken = stats.getRidesTaken();
                ratingCount = stats.getRatingCount();
                if (ratingCount > 0) {
                    rating = BigDecimal.valueOf(stats.getRatingSum())
                            .divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP)
                            .doubleValue();
                }
            }
        }

        if (account != null) {
            emailVerified = account.getEmailVerifiedAt() != null;
            phoneVerified = account.getPhoneVerifiedAt() != null;
        }

        AccountType accountType = profile != null ? profile.getAccountType() : AccountType.PRIVATE;

        return new UserCardDto(userId, name, rating, ridesGiven + ridesTaken,
                avatarUrl, bio, emailVerified, phoneVerified,
                ridesGiven, ridesTaken, ratingCount, vehicles, accountType);
    }
}
