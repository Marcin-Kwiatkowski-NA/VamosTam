package com.blablatwo.messaging;

import com.blablatwo.messaging.dto.ConversationResponseDto;
import com.blablatwo.messaging.dto.PeerUserDto;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import org.springframework.stereotype.Component;

@Component
public class ConversationDtoBuilder {

    private final UserProfileRepository userProfileRepository;

    public ConversationDtoBuilder(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
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

        return new ConversationResponseDto(
                conversation.getId(),
                conversation.getTopicKey(),
                peerUser,
                conversation.getLastMessageBody(),
                conversation.getLastMessageCreatedAt(),
                unreadCount
        );
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
