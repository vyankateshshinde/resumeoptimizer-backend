package com.vyankatesh.resumeoptimizer.jobfinder.dto.request;

import jakarta.validation.constraints.NotNull;

public record JobAlertRequest(
        @NotNull Long preferenceId,
        Boolean enabled
) {
}
