package com.vyankatesh.resumeoptimizer.dashboard.controller;

import com.vyankatesh.resumeoptimizer.dashboard.dto.DashboardResponse;
import com.vyankatesh.resumeoptimizer.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse getDashboard() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User not authenticated"
            );
        }

        String email = authentication.getName();

        System.out.println("DASHBOARD API HIT");
        System.out.println("EMAIL = " + email);

        return dashboardService.getDashboardData(email);
    }
}
