package com.yang.moni.auth;

import com.yang.moni.security.JwtTokenProvider;
import com.yang.moni.user.User;
import com.yang.moni.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final String naverClientId;
    private final String naverClientSecret;

    private final RestClient kakaoApiClient = RestClient.create("https://kapi.kakao.com");
    private final RestClient naverTokenClient = RestClient.create("https://nid.naver.com");
    private final RestClient naverApiClient = RestClient.create("https://openapi.naver.com");

    public AuthService(
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            @Value("${naver.client-id}") String naverClientId,
            @Value("${naver.client-secret}") String naverClientSecret) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.naverClientId = naverClientId;
        this.naverClientSecret = naverClientSecret;
    }

    @Transactional
    public AuthResponse loginWithKakao(String accessToken) {
        KakaoUserResponse kakaoUser;
        try {
            kakaoUser = kakaoApiClient.get()
                    .uri("/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("카카오 인증에 실패했습니다");
        }

        KakaoUserResponse.KakaoAccount account = kakaoUser.kakaoAccount();
        KakaoUserResponse.KakaoProfile profile = account != null ? account.profile() : null;

        User user = findOrCreateUser(
                "KAKAO",
                String.valueOf(kakaoUser.id()),
                account != null ? account.email() : null,
                profile != null ? profile.nickname() : null,
                profile != null ? profile.profileImageUrl() : null
        );
        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithNaver(String code) {
        NaverTokenResponse tokenResponse;
        NaverUserResponse userResponse;
        try {
            tokenResponse = naverTokenClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/oauth2.0/token")
                            .queryParam("grant_type", "authorization_code")
                            .queryParam("client_id", naverClientId)
                            .queryParam("client_secret", naverClientSecret)
                            .queryParam("code", code)
                            .build())
                    .retrieve()
                    .body(NaverTokenResponse.class);

            userResponse = naverApiClient.get()
                    .uri("/v1/nid/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.accessToken())
                    .retrieve()
                    .body(NaverUserResponse.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("네이버 인증에 실패했습니다");
        }

        NaverUserResponse.NaverProfile profile = userResponse.response();
        User user = findOrCreateUser(
                "NAVER", profile.id(), profile.email(), profile.nickname(), profile.profileImage()
        );
        return toAuthResponse(user);
    }

    private User findOrCreateUser(String provider, String providerId, String email,
                                   String nickname, String profileImageUrl) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .provider(provider)
                        .providerId(providerId)
                        .email(email)
                        .nickname(nickname != null ? nickname : provider + "_" + providerId)
                        .profileImageUrl(profileImageUrl)
                        .build()));
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user.getUserId());
        return new AuthResponse(token, user.getUserId(), user.getNickname());
    }
}
