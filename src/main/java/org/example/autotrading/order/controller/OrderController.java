package org.example.autotrading.order.controller;


import lombok.RequiredArgsConstructor;
import org.example.autotrading.order.dto.MockOrderRequestDto;
import org.example.autotrading.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/mock/order")
    public ResponseEntity<?> mockOrder(@RequestBody MockOrderRequestDto mockOrderRequestDto) throws IOException {
        return orderService.mockOrder(mockOrderRequestDto);
    }
}
