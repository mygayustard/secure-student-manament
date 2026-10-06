package com.example.studentmanagement.user;

import java.time.Instant;
import java.util.Set;

public record UserResponse(Long id, String username, String email, Set<Role> roles, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                Set.copyOf(user.getRoles()), user.getCreatedAt());
    }
}