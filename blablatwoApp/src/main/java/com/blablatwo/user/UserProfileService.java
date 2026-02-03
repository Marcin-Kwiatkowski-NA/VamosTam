package com.blablatwo.user;

import com.blablatwo.user.dto.UpdateProfileRequest;
import com.blablatwo.user.dto.UserProfileDto;
import com.blablatwo.user.exception.NoSuchUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        return userProfileMapper.toDto(account, profile);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.phoneNumber() != null) {
            profile.setPhoneNumber(request.phoneNumber());
        }

        userProfileRepository.save(profile);

        return userProfileMapper.toDto(account, profile);
    }
}
