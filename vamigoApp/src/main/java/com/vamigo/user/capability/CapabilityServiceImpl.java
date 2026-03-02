package com.vamigo.user.capability;

import com.vamigo.user.AccountStatus;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.exception.NoSuchUserException;
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
        return isActive(userId);
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
