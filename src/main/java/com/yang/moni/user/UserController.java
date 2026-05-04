package com.yang.moni.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public List<UserResponse> getUsers() {
        return userRepository.findByActiveTrue().stream()
                .map(u -> new UserResponse(u.getUserId(), u.getNickname()))
                .toList();
    }
}
