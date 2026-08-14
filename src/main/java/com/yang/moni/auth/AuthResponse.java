package com.yang.moni.auth;

public record AuthResponse(String token, Long userId, String nickname) {
}
