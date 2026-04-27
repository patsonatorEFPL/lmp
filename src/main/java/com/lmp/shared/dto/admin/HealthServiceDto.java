package com.lmp.shared.dto.admin;

public record HealthServiceDto(
    String name,
    String status,
    String latency,
    String uptime,
    String tone
) {}
