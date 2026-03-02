package com.vamigo.notification;

import com.vamigo.notification.dto.DeviceTokenRequest;
import com.vamigo.notification.dto.DeviceTokenResponse;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.exception.NoSuchUserException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserAccountRepository userAccountRepository;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository,
                               UserAccountRepository userAccountRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public DeviceTokenResponse register(Long userId, DeviceTokenRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        Instant now = Instant.now();
        DeviceToken token = deviceTokenRepository.findByToken(request.token())
                .map(existing -> {
                    existing.setUser(user);
                    existing.setPlatform(request.platform());
                    existing.setUpdatedAt(now);
                    return existing;
                })
                .orElseGet(() -> DeviceToken.builder()
                        .user(user)
                        .token(request.token())
                        .platform(request.platform())
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        DeviceToken saved = deviceTokenRepository.save(token);
        return toResponse(saved);
    }

    @Transactional
    public void unregister(Long userId, Long tokenId) {
        deviceTokenRepository.deleteByUserIdAndId(userId, tokenId);
    }

    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> getTokensForUser(Long userId) {
        return deviceTokenRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private DeviceTokenResponse toResponse(DeviceToken token) {
        return new DeviceTokenResponse(token.getId(), token.getPlatform(), token.getCreatedAt());
    }
}
