package org.example.autotrading.order.dto;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockOrderRequestDto {
    private String market;
    private String side;
    private String orderType;
    private BigDecimal price;
    private BigDecimal volume;
}
