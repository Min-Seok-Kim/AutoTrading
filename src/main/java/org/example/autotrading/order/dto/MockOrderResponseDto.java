package org.example.autotrading.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal executedPrice,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal totalPrice
) {}
