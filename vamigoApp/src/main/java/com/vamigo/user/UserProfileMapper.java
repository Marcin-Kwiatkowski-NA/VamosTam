package com.vamigo.user;

import com.vamigo.config.StorageProperties;
import com.vamigo.user.dto.UserProfileDto;
import com.vamigo.user.dto.UserStatsDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring")
public abstract class UserProfileMapper {

    @Autowired
    private StorageProperties storageProperties;

    @Mapping(target = "id", source = "account.id")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "status", source = "account.status")
    @Mapping(target = "stats", source = "profile.stats", qualifiedByName = "toStatsDto")
    @Mapping(target = "avatarUrl", expression = "java(deriveAvatarUrl(profile))")
    public abstract UserProfileDto toDto(UserAccount account, UserProfile profile);

    String deriveAvatarUrl(UserProfile profile) {
        if (profile.getAvatarObjectKey() != null) {
            return storageProperties.publicUrlBase() + "/" + profile.getAvatarObjectKey();
        }
        return null;
    }

    @Named("toStatsDto")
    UserStatsDto toStatsDto(UserStats stats) {
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
