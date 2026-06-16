package com.vyankatesh.resumeoptimizer.auth;

import com.vyankatesh.resumeoptimizer.dto.AuthResponse;
import com.vyankatesh.resumeoptimizer.dto.LoginRequest;
import com.vyankatesh.resumeoptimizer.dto.RegisterRequest;
import com.vyankatesh.resumeoptimizer.security.JwtService;
import com.vyankatesh.resumeoptimizer.user.User;
import com.vyankatesh.resumeoptimizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;   // ✅ ADDED JWT SERVICE

    // REGISTER
    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        return "User registered successfully";
    }

    // LOGIN (JWT ENABLED)
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("EMAIL FROM REQUEST = " + request.getEmail());
        System.out.println("PASSWORD FROM REQUEST = " + request.getPassword());
        System.out.println("PASSWORD FROM DB = " + user.getPassword());

        boolean matches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        System.out.println("PASSWORD MATCHES = " + matches);

        if (!matches) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token);
    }}