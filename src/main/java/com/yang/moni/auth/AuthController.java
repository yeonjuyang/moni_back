package com.yang.moni.auth;

import com.yang.moni.security.JwtTokenProvider;
import com.yang.moni.user.User;
import com.yang.moni.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    // 카카오/네이버 개발자 앱 키 발급 전까지 사용하는 임시 로그인.
    // 이메일로 유저를 찾거나 새로 만들고 JWT를 발급한다.
    @PostMapping("/dev-login")
    public AuthResponse devLogin(@RequestBody DevLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(request.email())
                        .nickname(request.nickname() != null ? request.nickname() : request.email())
                        .build()));

        String token = jwtTokenProvider.generateToken(user.getUserId());
        return new AuthResponse(token, user.getUserId(), user.getNickname());
    }

    // 프론트(kakao_flutter_sdk_user)가 카카오 SDK로 로그인해 발급받은 accessToken을 그대로 전달한다.
    @PostMapping("/kakao")
    public AuthResponse kakaoLogin(@RequestParam String accessToken) {
        return authService.loginWithKakao(accessToken);
    }

    // 프론트(flutter_web_auth_2)가 네이버 인가 화면에서 받아온 authorization code를 전달한다.
    @PostMapping("/naver")
    public AuthResponse naverLogin(@RequestParam String code) {
        return authService.loginWithNaver(code);
    }
}
