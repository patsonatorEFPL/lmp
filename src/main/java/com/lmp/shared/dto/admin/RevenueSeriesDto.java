package com.lmp.shared.dto.admin;

import java.util.List;

public record RevenueSeriesDto(
    List<Number> current,
    List<Number> previous,
    List<String> labels
) {}
