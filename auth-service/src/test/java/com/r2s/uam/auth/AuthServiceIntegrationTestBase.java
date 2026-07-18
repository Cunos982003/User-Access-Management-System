package com.r2s.uam.auth;

import com.r2s.uam.auth.entity.Authority;
import com.r2s.uam.auth.entity.OtpCode;
import com.r2s.uam.auth.entity.OtpType;
import com.r2s.uam.auth.entity.RefreshToken;
import com.r2s.uam.auth.entity.Role;
import com.r2s.uam.auth.entity.User;
import com.r2s.uam.auth.entity.UserStatus;
import com.r2s.uam.auth.repository.OtpCodeRepository;
import com.r2s.uam.auth.repository.RefreshTokenRepository;
import com.r2s.uam.auth.repository.RoleRepository;
import com.r2s.uam.auth.repository.UserRepository;
import com.r2s.uam.auth.service.AuditLogService;
import com.r2s.uam.auth.service.OtpService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class AuthServiceIntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected OtpCodeRepository otpCodeRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected OtpService otpService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @MockBean
    protected AuditLogService auditLogService;

    @MockBean
    protected JavaMailSender mailSender;

    protected void configureMailSenderMock() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
    }

    protected Role defaultRole;

    protected void setUpRole() {
        Authority readAuthority = Authority.builder()
            .id(1)
            .name("READ_USER")
            .build();
        Authority updateAuthority = Authority.builder()
            .id(2)
            .name("UPDATE_USER")
            .build();

        Role roleUser = Role.builder()
            .name("ROLE_USER")
            .authorities(Set.of(readAuthority, updateAuthority))
            .build();
        defaultRole = roleRepository.save(roleUser);

        Authority lockAuthority = Authority.builder()
            .id(3)
            .name("LOCK_USER")
            .build();
        Role roleModerator = Role.builder()
            .name("ROLE_MODERATOR")
            .authorities(Set.of(lockAuthority))
            .build();
        roleRepository.save(roleModerator);

        Authority exportAuthority = Authority.builder()
            .id(4)
            .name("EXPORT_AUDIT")
            .build();
        Role roleAdmin = Role.builder()
            .name("ROLE_ADMIN")
            .authorities(Set.of(exportAuthority))
            .build();
        roleRepository.save(roleAdmin);
    }

    protected User createAndSaveUser(String username, String email, String password, UserStatus status) {
        Set<Role> roles = new HashSet<>();
        if (defaultRole == null) {
            setUpRole();
        }
        roles.add(defaultRole);

        User user = User.builder()
            .id(UUID.randomUUID())
            .username(username)
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .fullName("Test User")
            .phone("+1234567890")
            .status(status)
            .roles(roles)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        return userRepository.save(user);
    }

    protected User createActiveUser(String username, String email, String password) {
        return createAndSaveUser(username, email, password, UserStatus.ACTIVE);
    }

    protected User createPendingUser(String username, String email, String password) {
        return createAndSaveUser(username, email, password, UserStatus.PENDING);
    }

    protected OtpCode createAndSaveOtpCode(User user, String code, OtpType type) {
        OtpCode otpCode = OtpCode.builder()
            .id(UUID.randomUUID())
            .user(user)
            .code(code)
            .type(type)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .used(false)
            .createdAt(LocalDateTime.now())
            .build();
        return otpCodeRepository.save(otpCode);
    }

    protected RefreshToken createAndSaveRefreshToken(User user, boolean revoked, LocalDateTime expiresAt) {
        RefreshToken token = RefreshToken.builder()
            .id(UUID.randomUUID())
            .user(user)
            .token("test-refresh-" + UUID.randomUUID())
            .deviceId(UUID.randomUUID())
            .expiresAt(expiresAt != null ? expiresAt : LocalDateTime.now().plusDays(7))
            .revoked(revoked)
            .createdAt(LocalDateTime.now())
            .build();
        return refreshTokenRepository.save(token);
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        otpCodeRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }
}