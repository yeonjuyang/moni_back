package com.yang.moni.auth;

import com.yang.moni.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends ApiTestBase {

    @Test
    @DisplayName("AUTH-01: dev-login 신규 이메일 → 유저 생성 + JWT 발급")
    void devLogin_newEmail_createsUserAndReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"new@moni.dev\",\"nickname\":\"신규유저\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.nickname").value("신규유저"));
    }

    @Test
    @DisplayName("AUTH-02: dev-login 동일 이메일 재로그인 → 같은 userId 반환")
    void devLogin_sameEmail_returnsSameUserId() throws Exception {
        // setUp()에서 이미 tester@moni.dev로 로그인됨 → userId 기록됨
        String res = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"tester@moni.dev\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long returnedId = objectMapper.readTree(res).get("userId").asLong();
        assertThat(returnedId).isEqualTo(userId);
    }

    @Test
    @DisplayName("AUTH-03: JWT 없이 보호된 엔드포인트 접근 → 401")
    void accessProtectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/ledgers"))
                .andExpect(status().isUnauthorized());
    }
}
