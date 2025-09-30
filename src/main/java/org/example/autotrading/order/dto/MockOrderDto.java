package org.example.autotrading.order.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MockOrderDto {
    private String market;
    private String side;
    private double price;
    private double volume;
}
