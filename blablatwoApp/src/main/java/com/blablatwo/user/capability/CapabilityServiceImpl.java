package com.blablatwo.user.capability;

import com.blablatwo.user.AccountStatus;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.exception.NoSuchUserException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CapabilityServiceImpl implements CapabilityService {

    private final UserAccountRepository userAccountRepository;

    public CapabilityServiceImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canBook(Long userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        return account.getStatus() == AccountStatus.ACTIVE
                && account.getPhoneVerifiedAt() != null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCreateRide(Long userId) {
        return isActive(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        return account.getStatus() == AccountStatus.ACTIVE;
    }
}
