package com.momently.orchestrator.security;

import com.momently.orchestrator.config.MomentlySecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 설정에 정의된 콘솔 계정으로만 로그인을 허용한다.
 *
 * <p>다중 사용자·DB 연동 전 단계이다.</p>
 */
@Service
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-zA-Z0-9._-]{3,40}");

    private final MomentlySecurityProperties properties;
    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
        MomentlySecurityProperties properties,
        JwtService jwtService,
        UserAccountRepository userAccountRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<String> issueToken(String username, String password) {
        String normalizedUsername = username == null ? "" : username.strip();
        if (normalizedUsername.isBlank() || password == null) {
            return Optional.empty();
        }
        Optional<UserAccount> account = userAccountRepository.findByUsername(normalizedUsername);
        if (account.isPresent()) {
            if (passwordEncoder.matches(password, account.get().passwordHash())) {
                return Optional.of(jwtService.createAccessToken(normalizedUsername));
            }
            return Optional.empty();
        }
        if (!constantTimeEquals(properties.username(), normalizedUsername)) {
            return Optional.empty();
        }
        if (!constantTimeEquals(properties.password(), password)) {
            return Optional.empty();
        }
        return Optional.of(jwtService.createAccessToken(normalizedUsername));
    }

    public String registerAndIssueToken(String username, String password, String inviteCode) {
        if (!properties.signupEnabled()) {
            throw new SignupDisabledException();
        }
        if (!constantTimeEquals(properties.signupInviteCode(), inviteCode)) {
            throw new IllegalArgumentException("초대 코드가 올바르지 않습니다.");
        }
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);
        if (userAccountRepository.existsByUsername(normalizedUsername)
            || constantTimeEquals(properties.username(), normalizedUsername)) {
            throw new DuplicateUsernameException(normalizedUsername);
        }
        UserAccount account = new UserAccount(
            normalizedUsername,
            passwordEncoder.encode(password),
            Instant.now()
        );
        userAccountRepository.save(account);
        return jwtService.createAccessToken(normalizedUsername);
    }

    private static String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.strip();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("아이디는 영문, 숫자, '.', '_', '-' 조합 3~40자로 입력하세요.");
        }
        return normalized;
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 120) {
            throw new IllegalArgumentException("비밀번호는 8~120자로 입력하세요.");
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
