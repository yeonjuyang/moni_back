package com.yang.moni.auth;

import com.yang.moni.security.JwtTokenProvider;
import com.yang.moni.user.User;
import com.yang.moni.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 카카오/네이버 개발자 앱 키 발급 전까지 사용하는 임시 로그인.
    // 이메일로 유저를 찾거나 새로 만들고 JWT를 발급한다.
    // 키가 준비되면 /api/auth/kakao, /api/auth/naver 를 추가해 같은 JwtTokenProvider로 토큰만 발급하면 된다.
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
}
