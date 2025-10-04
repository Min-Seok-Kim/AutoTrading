package org.example.autotrading.order.controller;


import lombok.RequiredArgsConstructor;
import org.example.autotrading.order.dto.MockOrderRequestDto;
import org.example.autotrading.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/mock/order")
    public ResponseEntity<?> mockOrder(@RequestBody MockOrderRequestDto mockOrderRequestDto) throws IOException {
        return orderService.mockOrder(mockOrderRequestDto);
    }

    @PostMapping("/sell-rvn")
    public ResponseEntity<?> sellRvn(@RequestParam double volume) throws NoSuchAlgorithmException {
        return orderService.sellRvn(volume);
    }

    @PostMapping("/buy-rvn")
    public ResponseEntity<?> buyRvn(@RequestParam double volume) throws NoSuchAlgorithmException {
        return orderService.buyRvn(volume);
    }
}
