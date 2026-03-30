package com.vamigo.ride;

import com.vamigo.domain.PersonDisplayNameResolver;
import com.vamigo.dto.UserCardDto;
import com.vamigo.location.LocationMapper;
import com.vamigo.ride.dto.BookingResponseDto;
import com.vamigo.ride.dto.RideSummaryDto;
import com.vamigo.user.AccountType;
import com.vamigo.user.AvatarUrlResolver;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileRepository;
import com.vamigo.user.UserStats;
import com.vamigo.vehicle.VehicleMapper;
import com.vamigo.vehicle.VehicleRepository;
import com.vamigo.vehicle.VehicleResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class BookingResponseEnricher {

    private final UserProfileRepository userProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final PersonDisplayNameResolver displayNameResolver;
    private final LocationMapper locationMapper;
    private final AvatarUrlResolver avatarUrlResolver;

    public BookingResponseEnricher(UserProfileRepository userProfileRepository,
                                    UserAccountRepository userAccountRepository,
                                    VehicleRepository vehicleRepository,
                                    VehicleMapper vehicleMapper,
                                    PersonDisplayNameResolver displayNameResolver,
                                    LocationMapper locationMapper,
                                    AvatarUrlResolver avatarUrlResolver) {
        this.userProfileRepository = userProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
        this.displayNameResolver = displayNameResolver;
        this.locationMapper = locationMapper;
        this.avatarUrlResolver = avatarUrlResolver;
    }

    public BookingResponseDto enrich(RideBooking booking, BookingResponseDto dto) {
        Long passengerId = booking.getPassenger().getId();
        UserCardDto userCard = buildUserCard(passengerId);
        return dto.toBuilder().passenger(userCard).build();
    }

    public List<BookingResponseDto> enrich(List<RideBooking> bookings, List<BookingResponseDto> dtos) {
        Set<Long> passengerIds = bookings.stream()
                .map(b -> b.getPassenger().getId())
                .collect(Collectors.toSet());

        if (passengerIds.isEmpty()) return dtos;

        Map<Long, UserProfile> profilesById = StreamSupport.stream(
                        userProfileRepository.findAllById(passengerIds).spliterator(), false)
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));

        Map<Long, UserAccount> accountsById = userAccountRepository.findAllById(passengerIds).stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));

        Map<Long, List<VehicleResponseDto>> vehiclesByOwnerId =
                vehicleRepository.findByOwnerIdIn(passengerIds).stream()
                        .collect(Collectors.groupingBy(
                                v -> v.getOwner().getId(),
                                Collectors.mapping(vehicleMapper::vehicleEntityToVehicleResponseDto,
                                        Collectors.toList())));

        List<BookingResponseDto> result = new ArrayList<>(bookings.size());
        for (int i = 0; i < bookings.size(); i++) {
            Long pid = bookings.get(i).getPassenger().getId();
            UserCardDto userCard = buildUserCard(
                    pid,
                    profilesById.get(pid),
                    accountsById.get(pid),
                    vehiclesByOwnerId.getOrDefault(pid, List.of()));
            result.add(dtos.get(i).toBuilder().passenger(userCard).build());
        }
        return result;
    }

    public List<BookingResponseDto> enrichForPassenger(List<RideBooking> bookings, List<BookingResponseDto> dtos) {
        Set<Long> driverIds = bookings.stream()
                .map(b -> b.getRide().getDriver().getId())
                .collect(Collectors.toSet());

        if (driverIds.isEmpty()) return dtos;

        Map<Long, UserProfile> profilesById = StreamSupport.stream(
                        userProfileRepository.findAllById(driverIds).spliterator(), false)
                .collect(Collectors.toMap(UserProfile::getId, Function.identity()));

        Map<Long, UserAccount> accountsById = userAccountRepository.findAllById(driverIds).stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));

        Map<Long, List<VehicleResponseDto>> vehiclesByOwnerId =
                vehicleRepository.findByOwnerIdIn(driverIds).stream()
                        .collect(Collectors.groupingBy(
                                v -> v.getOwner().getId(),
                                Collectors.mapping(vehicleMapper::vehicleEntityToVehicleResponseDto,
                                        Collectors.toList())));

        List<BookingResponseDto> result = new ArrayList<>(bookings.size());
        for (int i = 0; i < bookings.size(); i++) {
            Ride ride = bookings.get(i).getRide();
            Long driverId = ride.getDriver().getId();
            UserCardDto driverCard = buildUserCard(
                    driverId,
                    profilesById.get(driverId),
                    accountsById.get(driverId),
                    vehiclesByOwnerId.getOrDefault(driverId, List.of()));

            RideSummaryDto rideSummary = RideSummaryDto.builder()
                    .id(ride.getId())
                    .origin(locationMapper.locationToDto(ride.getOrigin()))
                    .destination(locationMapper.locationToDto(ride.getDestination()))
                    .departureTime(ride.getDepartureTime())
                    .pricePerSeat(ride.getPricePerSeat())
                    .totalSeats(ride.getTotalSeats())
                    .driver(driverCard)
                    .rideStatus(ride.getRideStatus())
                    .currency(ride.getCurrency())
                    .build();

            result.add(dtos.get(i).toBuilder().ride(rideSummary).build());
        }
        return result;
    }

    private UserCardDto buildUserCard(Long passengerId) {
        UserProfile profile = userProfileRepository.findById(passengerId).orElse(null);
        UserAccount account = userAccountRepository.findById(passengerId).orElse(null);
        List<VehicleResponseDto> vehicles = vehicleRepository.findByOwnerId(passengerId).stream()
                .map(vehicleMapper::vehicleEntityToVehicleResponseDto)
                .toList();
        return buildUserCard(passengerId, profile, account, vehicles);
    }

    private UserCardDto buildUserCard(Long id, UserProfile profile, UserAccount account,
                                       List<VehicleResponseDto> vehicles) {
        String name = displayNameResolver.resolveInternal(profile, id);

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

        return new UserCardDto(id, name, rating, ridesGiven + ridesTaken,
                avatarUrl, bio, emailVerified, phoneVerified,
                ridesGiven, ridesTaken, ratingCount, vehicles, accountType);
    }
}
