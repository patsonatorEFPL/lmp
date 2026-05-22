package com.lmp.shared.dto.admin;

import java.math.BigDecimal;

public record TopServiceDto(
    String name,
    long orders,
    BigDecimal revenue,
    int growthPercent
) {}
