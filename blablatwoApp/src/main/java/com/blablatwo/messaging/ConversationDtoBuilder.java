package com.blablatwo.messaging;

import com.blablatwo.messaging.dto.ConversationResponseDto;
import com.blablatwo.messaging.dto.OfferKind;
import com.blablatwo.messaging.dto.OfferStatus;
import com.blablatwo.messaging.dto.PeerUserDto;
import com.blablatwo.messaging.dto.RideContextDto;
import com.blablatwo.messaging.dto.ViewerRole;
import com.blablatwo.ride.Ride;
import com.blablatwo.ride.RideRepository;
import com.blablatwo.seat.Seat;
import com.blablatwo.seat.SeatRepository;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
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

    public ConversationDtoBuilder(UserProfileRepository userProfileRepository,
                                   RideRepository rideRepository,
                                   SeatRepository seatRepository) {
        this.userProfileRepository = userProfileRepository;
        this.rideRepository = rideRepository;
        this.seatRepository = seatRepository;
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
        String avatarUrl = null;

        if (profile != null) {
            displayName = (profile.getDisplayName() != null && !profile.getDisplayName().isBlank())
                    ? profile.getDisplayName()
                    : user.getEmail().split("@")[0];
            avatarUrl = profile.getAvatarUrl();
        } else {
            displayName = user.getEmail().split("@")[0];
        }

        return new PeerUserDto(user.getId(), displayName, avatarUrl);
    }
}
