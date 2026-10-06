package com.example.studentmanagement.auth;

import com.example.studentmanagement.exception.DuplicateResourceException;
import com.example.studentmanagement.security.JwtService;
import com.example.studentmanagement.user.Role;
import com.example.studentmanagement.user.User;
import com.example.studentmanagement.user.UserRepository;
import com.example.studentmanagement.user.UserResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.getRoles().add(Role.STUDENT);

        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("Username or email is already registered");
        }
    }

    public LoginResponse login(LoginRequest request) {
        // Throws BadCredentialsException (-> 401) for unknown user OR wrong password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsernameIgnoreCase(request.username())
            .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRoles());
        String refreshToken = refreshTokenService.issue(user);

        return new LoginResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessExpirationSeconds());
    }

    public LoginResponse refresh(String refreshTokenValue) {
        RefreshTokenService.TokenRotation rotation = refreshTokenService.rotate(refreshTokenValue);
        String newAccessToken = jwtService.generateAccessToken(
            rotation.username(), rotation.roles());
        return new LoginResponse(newAccessToken, rotation.refreshToken(), "Bearer",
            jwtService.getAccessExpirationSeconds());
    }

    public void logout(String refreshTokenValue) {
        // Revoke refresh only; stateless access tokens remain valid until they expire.
        refreshTokenService.revoke(refreshTokenValue);
    }
}
