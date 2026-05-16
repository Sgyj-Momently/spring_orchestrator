package com.momently.orchestrator.adapter.in.web;

import com.momently.orchestrator.adapter.in.web.request.LoginRequest;
import com.momently.orchestrator.adapter.in.web.request.RegisterRequest;
import com.momently.orchestrator.adapter.in.web.response.LoginResponse;
import com.momently.orchestrator.config.MomentlySecurityProperties;
import com.momently.orchestrator.security.AuthService;
import com.momently.orchestrator.security.DuplicateUsernameException;
import com.momently.orchestrator.security.SignupDisabledException;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 콘솔 로그인·토큰 발급이다.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final MomentlySecurityProperties securityProperties;

    public AuthController(AuthService authService, MomentlySecurityProperties securityProperties) {
        this.authService = authService;
        this.securityProperties = securityProperties;
    }

    /**
     * 아이디·비밀이 맞으면 JWT를 발급한다.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<String> token = authService.issueToken(request.username(), request.password());
        if (token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }
        return ResponseEntity.ok(new LoginResponse(
            token.get(),
            "Bearer",
            securityProperties.jwtExpirationSeconds()));
    }

    /**
     * 초대 코드가 맞으면 사용자 계정을 만들고 JWT를 발급한다.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String token = authService.registerAndIssueToken(
                request.username(),
                request.password(),
                request.inviteCode()
            );
            return ResponseEntity.ok(new LoginResponse(
                token,
                "Bearer",
                securityProperties.jwtExpirationSeconds()));
        } catch (SignupDisabledException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
        } catch (DuplicateUsernameException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "이미 사용 중인 아이디입니다."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", exception.getMessage()));
        }
    }
}
