package com.vamigo.messaging;

import com.vamigo.messaging.dto.ConversationResponseDto;
import com.vamigo.messaging.dto.OfferKind;
import com.vamigo.messaging.dto.OfferStatus;
import com.vamigo.messaging.dto.PeerUserDto;
import com.vamigo.messaging.dto.RideContextDto;
import com.vamigo.messaging.dto.ViewerRole;
import com.vamigo.ride.RideRepository;
import com.vamigo.seat.SeatRepository;
import com.vamigo.user.AvatarUrlResolver;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ConversationDtoBuilder {

    private static final Logger log = LoggerFactory.getLogger(ConversationDtoBuilder.class);
    private static final Pattern TOPIC_KEY_PATTERN = Pattern.compile("^offer:([rs])-(\\d+)$");

    private final UserProfileRepository userProfileRepository;
    private final RideRepository rideRepository;
    private final SeatRepository seatRepository;
    private final AvatarUrlResolver avatarUrlResolver;

    public ConversationDtoBuilder(UserProfileRepository userProfileRepository,
                                   RideRepository rideRepository,
                                   SeatRepository seatRepository,
                                   AvatarUrlResolver avatarUrlResolver) {
        this.userProfileRepository = userProfileRepository;
        this.rideRepository = rideRepository;
        this.seatRepository = seatRepository;
        this.avatarUrlResolver = avatarUrlResolver;
    }

    public ConversationResponseDto toResponseDto(Conversation conversation, Long viewerId) {
        boolean isParticipantA = conversation.getParticipantA().getId().equals(viewerId);

        UserAccount peer = isParticipantA
                ? conversation.getParticipantB()
                : conversation.getParticipantA();

        PeerUserDto peerUser = buildPeerUserDto(peer);

        int unreadCount = isParticipantA
                ? conversation.getParticipantAUnreadCount()
                : conversation.getParticipantBUnreadCount();

        RideContextDto rideContext = resolveRideContext(conversation.getTopicKey(), viewerId);

        return new ConversationResponseDto(
                conversation.getId(),
                conversation.getTopicKey(),
                peerUser,
                conversation.getLastMessageBody(),
                conversation.getLastMessageCreatedAt(),
                unreadCount,
                rideContext
        );
    }

    private RideContextDto resolveRideContext(String topicKey, Long viewerId) {
        if (topicKey == null) return null;

        Matcher matcher = TOPIC_KEY_PATTERN.matcher(topicKey);
        if (!matcher.matches()) return null;

        String kindChar = matcher.group(1);
        long offerId;
        try {
            offerId = Long.parseLong(matcher.group(2));
        } catch (NumberFormatException e) {
            return null;
        }

        try {
            if ("r".equals(kindChar)) {
                return resolveRideContext(offerId, viewerId);
            } else {
                return resolveSeatContext(offerId, viewerId);
            }
        } catch (Exception e) {
            log.debug("Failed to resolve ride context for topicKey={}: {}", topicKey, e.getMessage());
            return null;
        }
    }

    private RideContextDto resolveRideContext(long rideId, Long viewerId) {
        return rideRepository.findById(rideId)
                .map(ride -> {
                    Long driverId = ride.getDriver().getId();
                    boolean isCreator = viewerId.equals(driverId);
                    ViewerRole viewerRole = isCreator ? ViewerRole.DRIVER : ViewerRole.PASSENGER;

                    return new RideContextDto(
                            OfferKind.RIDE,
                            rideId,
                            ride.getOrigin().getName(null),
                            ride.getDestination().getName(null),
                            ride.getDepartureTime(),
                            OfferStatus.from(ride.getRideStatus()),
                            viewerRole,
                            isCreator
                    );
                })
                .orElse(null);
    }

    private RideContextDto resolveSeatContext(long seatId, Long viewerId) {
        return seatRepository.findById(seatId)
                .map(seat -> {
                    Long passengerId = seat.getPassenger().getId();
                    boolean isCreator = viewerId.equals(passengerId);
                    ViewerRole viewerRole = isCreator ? ViewerRole.PASSENGER : ViewerRole.DRIVER;

                    return new RideContextDto(
                            OfferKind.SEAT,
                            seatId,
                            seat.getOrigin().getName(null),
                            seat.getDestination().getName(null),
                            seat.getDepartureTime(),
                            OfferStatus.from(seat.computeSeatStatus()),
                            viewerRole,
                            isCreator
                    );
                })
                .orElse(null);
    }

    private PeerUserDto buildPeerUserDto(UserAccount user) {
        UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);

        String displayName;
        if (profile != null) {
            displayName = (profile.getDisplayName() != null && !profile.getDisplayName().isBlank())
                    ? profile.getDisplayName()
                    : user.getEmail().split("@")[0];
        } else {
            displayName = user.getEmail().split("@")[0];
        }

        return new PeerUserDto(user.getId(), displayName, avatarUrlResolver.resolve(profile));
    }
}
