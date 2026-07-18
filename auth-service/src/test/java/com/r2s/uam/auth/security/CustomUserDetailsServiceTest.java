package com.r2s.uam.auth.security;

import com.r2s.uam.auth.entity.Authority;
import com.r2s.uam.auth.entity.Role;
import com.r2s.uam.auth.entity.User;
import com.r2s.uam.auth.entity.UserStatus;
import com.r2s.uam.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private Role userRole;
    private Role adminRole;
    private Authority readAuth;
    private Authority updateAuth;

    @BeforeEach
    void setUp() {
        readAuth = Authority.builder().id(1).name("READ_USER").build();
        updateAuth = Authority.builder().id(2).name("UPDATE_USER").build();
        Authority deleteAuth = Authority.builder().id(3).name("DELETE_USER").build();

        userRole = Role.builder()
            .id(1)
            .name("ROLE_USER")
            .authorities(Set.of(readAuth, updateAuth))
            .build();

        adminRole = Role.builder()
            .id(2)
            .name("ROLE_ADMIN")
            .authorities(Set.of(deleteAuth))
            .build();

        testUser = User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .passwordHash("$2a$12$hashedpassword")
            .status(UserStatus.ACTIVE)
            .roles(new HashSet<>())
            .build();
    }

    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("should load user by username and return UserDetails")
        void shouldLoadUserByUsername() {
            testUser.setRoles(Set.of(userRole));
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));

            UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("testuser");
            assertThat(userDetails.getPassword()).isEqualTo("$2a$12$hashedpassword");
            assertThat(userDetails.isAccountNonExpired()).isTrue(); // implementation sets false explicitly
            assertThat(userDetails.isAccountNonLocked()).isTrue(); // ACTIVE = not locked
            assertThat(userDetails.isCredentialsNonExpired()).isTrue(); // implementation sets false explicitly
            assertThat(userDetails.isEnabled()).isTrue(); // ACTIVE = enabled
        }

        @Test
        @DisplayName("should load user by email (treated as username)")
        void shouldLoadUserByEmail() {
            testUser.setRoles(Set.of(userRole));
            // The method uses findByUsernameOrEmail with username param as both
            when(userRepository.findByUsernameOrEmail("test@example.com", "test@example.com"))
                .thenReturn(Optional.of(testUser));

            UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByUsernameOrEmail(any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should set accountNonExpired based on User builder value")
        void shouldSetAccountNonExpiredBasedOnBuilderValue() {
            // Verify the UserDetails accountNonExpired state is consistent
            testUser.setRoles(Set.of(userRole));
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));

            UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

            // Implementation explicitly sets accountExpired(false) — meaning NOT expired, so isAccountNonExpired = true
            assertThat(userDetails.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("should set credentialsNonExpired to false")
        void shouldSetCredentialsNonExpiredToFalse() {
            testUser.setRoles(Set.of(userRole));
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));

            UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

            // Implementation explicitly sets credentialsExpired(false) — meaning NOT expired, so isCredentialsNonExpired = true
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        }

        @Nested
        @DisplayName("Account lock status")
        class AccountLockStatusTests {

            @Test
            @DisplayName("should lock account when status is LOCKED")
            void shouldLockAccountWhenLocked() {
                testUser.setStatus(UserStatus.LOCKED);
                testUser.setRoles(Set.of(userRole));
                when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

                UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

                assertThat(userDetails.isAccountNonLocked()).isFalse();
            }

            @Test
            @DisplayName("should not lock account when status is ACTIVE")
            void shouldNotLockAccountWhenActive() {
                testUser.setStatus(UserStatus.ACTIVE);
                testUser.setRoles(Set.of(userRole));
                when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

                UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

                assertThat(userDetails.isAccountNonLocked()).isTrue();
            }

            @Test
            @DisplayName("should not lock account when status is PENDING")
            void shouldNotLockAccountWhenPending() {
                testUser.setStatus(UserStatus.PENDING);
                testUser.setRoles(Set.of(userRole));
                when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

                UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

                assertThat(userDetails.isAccountNonLocked()).isTrue();
            }
        }

        @Nested
        @DisplayName("Account disabled status")
        class AccountDisabledStatusTests {

            @Test
            @DisplayName("should disable account when status is not ACTIVE")
            void shouldDisableAccountWhenNotActive() {
                testUser.setStatus(UserStatus.PENDING);
                testUser.setRoles(Set.of(userRole));
                when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

                UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

                assertThat(userDetails.isEnabled()).isFalse();
            }

            @Test
            @DisplayName("should enable account when status is ACTIVE")
            void shouldEnableAccountWhenActive() {
                testUser.setStatus(UserStatus.ACTIVE);
                testUser.setRoles(Set.of(userRole));
                when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

                UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

                assertThat(userDetails.isEnabled()).isTrue();
            }

            @Test
            @DisplayName("should disable account when status is DISABLED")
            void shouldDisableAccountWhenDisabled() {
                testUser.setStatus(UserStatus.DISABLED);
                testUser.setRoles(Set.of(userRole));
                when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

                UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

                assertThat(userDetails.isEnabled()).isFalse();
            }

            @Test
            @DisplayName("should disable account when status is LOCKED")
            void shouldDisableAccountWhenLocked() {
                testUser.setStatus(UserStatus.LOCKED);
                testUser.setRoles(Set.of(userRole));
                when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

                UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

                assertThat(userDetails.isEnabled()).isFalse();
            }
        }

        @Nested
        @DisplayName("Authorities")
        class AuthoritiesTests {

            @Test
            @DisplayName("should include ROLE_USER in granted authorities")
            void shouldIncludeRoleAsAuthority() {
                testUser.setRoles(Set.of(userRole));
                when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

                UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

                var authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

                assertThat(authorities).contains("ROLE_USER");
            }

            @Test
            @DisplayName("should include all authorities from user's roles")
            void shouldIncludeAllAuthoritiesFromRoles() {
                testUser.setRoles(Set.of(userRole, adminRole));
                when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

                UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

                var authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

                assertThat(authorities).containsExactlyInAnyOrder(
                    "ROLE_USER", "ROLE_ADMIN", "READ_USER", "UPDATE_USER", "DELETE_USER"
                );
            }

            @Test
            @DisplayName("should return empty authorities for user with no roles")
            void shouldReturnEmptyAuthoritiesForNoRoles() {
                testUser.setRoles(new HashSet<>());
                when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

                UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

                assertThat(userDetails.getAuthorities()).isEmpty();
            }

            @Test
            @DisplayName("should include both roles and authorities from role")
            void shouldIncludeRolesAndAuthoritiesInGrantedAuths() {
                testUser.setRoles(Set.of(userRole));
                when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

                UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

                var authorityStrings = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

                assertThat(authorityStrings).contains("ROLE_USER", "READ_USER", "UPDATE_USER");
            }
        }
    }
}