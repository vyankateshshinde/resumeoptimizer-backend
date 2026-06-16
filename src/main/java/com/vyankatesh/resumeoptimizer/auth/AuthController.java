package com.vyankatesh.resumeoptimizer.auth;

import com.vyankatesh.resumeoptimizer.dto.AuthResponse;
import com.vyankatesh.resumeoptimizer.dto.LoginRequest;
import com.vyankatesh.resumeoptimizer.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // REGISTER API
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        String response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    // LOGIN API
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}