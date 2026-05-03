package com.momently.orchestrator.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MomentlySecurityPropertiesTest {

    @Test
    @DisplayName("만료 시간이 비양수면 일일 초로 안정화된다")
    void normalizesNonPositiveExpiry() {
        MomentlySecurityProperties props = new MomentlySecurityProperties(
            "u",
            "p",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            0);
        assertThat(props.jwtExpirationSeconds()).isEqualTo(86_400L);

        MomentlySecurityProperties neg = new MomentlySecurityProperties(
            "u",
            "p",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            -10);
        assertThat(neg.jwtExpirationSeconds()).isEqualTo(86_400L);
    }

    @Test
    @DisplayName("JWT 시크릿 검증 실패 시 설명 가능한 예외다")
    void validateJwtSecretStrengthRejectsWeakValues() {
        MomentlySecurityProperties blankSecret = new MomentlySecurityProperties("u", "p", "  ", 3600);
        assertThatThrownBy(blankSecret::validateJwtSecretStrength)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jwt-secret");

        MomentlySecurityProperties shortUtf8Secret = new MomentlySecurityProperties("u", "p", "일이삼사", 3600);
        assertThatThrownBy(shortUtf8Secret::validateJwtSecretStrength)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32");
    }
}
