package com.blablatwo.user;

import com.blablatwo.user.dto.UserProfileDto;
import com.blablatwo.user.dto.UserStatsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "id", source = "account.id")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "status", source = "account.status")
    @Mapping(target = "stats", source = "profile.stats", qualifiedByName = "toStatsDto")
    UserProfileDto toDto(UserAccount account, UserProfile profile);

    @Named("toStatsDto")
    default UserStatsDto toStatsDto(UserStats stats) {
        if (stats == null) {
            return new UserStatsDto(0, 0, null, 0);
        }

        BigDecimal ratingAvg = null;
        if (stats.getRatingCount() > 0) {
            ratingAvg = BigDecimal.valueOf(stats.getRatingSum())
                    .divide(BigDecimal.valueOf(stats.getRatingCount()), 2, RoundingMode.HALF_UP);
        }

        return new UserStatsDto(
                stats.getRidesGiven(),
                stats.getRidesTaken(),
                ratingAvg,
                stats.getRatingCount()
        );
    }
}
