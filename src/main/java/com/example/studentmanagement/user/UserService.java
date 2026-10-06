package com.example.studentmanagement.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;   
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(UserResponse::from)
            .orElseThrow(() -> new com.example.studentmanagement.exception.ResourceNotFoundException("User was not found"));
    }
}
