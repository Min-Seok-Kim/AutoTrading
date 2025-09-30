package org.example.autotrading.order.controller;


import lombok.RequiredArgsConstructor;
import org.example.autotrading.order.dto.MockOrderDto;
import org.example.autotrading.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/mock/order")
    public ResponseEntity<?> mockOrder(MockOrderDto mockOrderDto) {
        return orderService.mockOrder(mockOrderDto);
    }
}
