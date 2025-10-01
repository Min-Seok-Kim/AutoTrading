package org.example.autotrading.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;



public record MockOrderResponseDto(
        String market,
        String side,
        String orderType,
        BigDecimal price,
        BigDecimal volume,
        boolean executed,
        BigDecimal executedPrice
) {}
