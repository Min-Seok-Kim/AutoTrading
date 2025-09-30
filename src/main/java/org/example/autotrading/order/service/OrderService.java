package org.example.autotrading.order.service;


import lombok.RequiredArgsConstructor;
import org.example.autotrading.order.dto.MockOrderDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    public ResponseEntity<?> mockOrder(MockOrderDto mockOrderDto) {
        
    }
}
