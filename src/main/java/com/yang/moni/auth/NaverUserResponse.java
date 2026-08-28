package com.yang.moni.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverUserResponse(NaverProfile response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NaverProfile(String id, String email, String nickname,
                               @JsonProperty("profile_image") String profileImage) {
    }
}
