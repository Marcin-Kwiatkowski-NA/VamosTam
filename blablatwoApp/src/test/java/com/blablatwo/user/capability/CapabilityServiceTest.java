package com.blablatwo.user.capability;

import com.blablatwo.user.AccountStatus;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.exception.NoSuchUserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapabilityServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private CapabilityServiceImpl capabilityService;

    private static final Long USER_ID = 1L;

    @Nested
    class CanBook {

        @Test
        void returnsTrue_whenActiveAndPhoneVerified() {
            UserAccount account = UserAccount.builder()
                    .id(USER_ID)
                    .status(AccountStatus.ACTIVE)
                    .phoneVerifiedAt(Instant.now())
                    .build();
            when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(account));

            assertThat(capabilityService.canBook(USER_ID)).isTrue();
        }

        @Test
        void returnsFalse_whenActiveButPhoneNotVerified() {
            UserAccount account = UserAccount.builder()
                    .id(USER_ID)
                    .status(AccountStatus.ACTIVE)
                    .phoneVerifiedAt(null)
                    .build();
            when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(account));

            assertThat(capabilityService.canBook(USER_ID)).isFalse();
        }

        @Test
        void returnsFalse_whenDisabledWithPhoneVerified() {
            UserAccount account = UserAccount.builder()
                    .id(USER_ID)
                    .status(AccountStatus.DISABLED)
                    .phoneVerifiedAt(Instant.now())
                    .build();
            when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(account));

            assertThat(capabilityService.canBook(USER_ID)).isFalse();
        }

        @Test
        void returnsFalse_whenBannedWithPhoneVerified() {
            UserAccount account = UserAccount.builder()
                    .id(USER_ID)
                    .status(AccountStatus.BANNED)
                    .phoneVerifiedAt(Instant.now())
                    .build();
            when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(account));

            assertThat(capabilityService.canBook(USER_ID)).isFalse();
        }

        @Test
        void returnsFalse_whenDisabledAndPhoneNotVerified() {
            UserAccount account = UserAccount.builder()
                    .id(USER_ID)
                    .status(AccountStatus.DISABLED)
                    .phoneVerifiedAt(null)
                    .build();
            when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(account));

            assertThat(capabilityService.canBook(USER_ID)).isFalse();
        }

        @Test
        void throwsNoSuchUserException_whenUserNotFound() {
            when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> capabilityService.canBook(USER_ID))
                    .isInstanceOf(NoSuchUserException.class);
        }
    }

    @Nested
    class CanCreateRide {

        @Test
        void returnsTrue_whenActive() {
            UserAccount account = UserAccount.builder()
                    .id(USER_ID)
                    .status(AccountStatus.ACTIVE)
                    .build();
            when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(account));

            assertThat(capabilityService.canCreateRide(USER_ID)).isTrue();
        }

        @Test
        void returnsFalse_whenDisabled() {
            UserAccount account = UserAccount.builder()
                    .id(USER_ID)
                    .status(AccountStatus.DISABLED)
                    .build();
            when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(account));

            assertThat(capabilityService.canCreateRide(USER_ID)).isFalse();
        }

        @Test
        void returnsFalse_whenBanned() {
            UserAccount account = UserAccount.builder()
                    .id(USER_ID)
                    .status(AccountStatus.BANNED)
                    .build();
            when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(account));

            assertThat(capabilityService.canCreateRide(USER_ID)).isFalse();
        }

        @Test
        void throwsNoSuchUserException_whenUserNotFound() {
            when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> capabilityService.canCreateRide(USER_ID))
                    .isInstanceOf(NoSuchUserException.class);
        }
    }
}
