package org.example.autotrading.order.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.autotrading.order.dto.MockOrderRequestDto;
import org.example.autotrading.order.dto.MockOrderResponseDto;
import org.example.autotrading.order.entity.MockOrderEntity;
import org.example.autotrading.order.repository.MockOrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final ObjectMapper objectMapper;
    private final MockOrderRepository mockOrderRepository;

    public ResponseEntity<?> mockOrder(MockOrderRequestDto mockOrderRequestDto) throws IOException {
        OkHttpClient client = new OkHttpClient();

        String url = "https://api.upbit.com/v1/ticker?markets=" + mockOrderRequestDto.getMarket();

        System.out.println("url" + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .build();

        BigDecimal currentPrice;

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Ticker 조회 실패: " + response.code());
            }

            String body = Objects.requireNonNull(response.body()).string();

            // JSON 파싱 (예: [{"market":"KRW-BTC","trade_price":...}] 형식)
            JsonNode node = objectMapper.readTree(body);
            currentPrice = BigDecimal.valueOf(node.get(0).get("trade_price").asDouble());

        }

        boolean executed;
        BigDecimal executedPrice;

        if (mockOrderRequestDto.getOrderType().equals("MARKET")) {
            executed = true;
            executedPrice = currentPrice;
        } else {
            if (mockOrderRequestDto.getSide().equals("BUY")) {
                executed = mockOrderRequestDto.getPrice().compareTo(currentPrice) >= 0;
            } else { // SELL
                executed = mockOrderRequestDto.getPrice().compareTo(currentPrice) <= 0;
            }
            executedPrice = executed ? mockOrderRequestDto.getPrice() : BigDecimal.ZERO;
        }


        MockOrderEntity mockOrderEntity = MockOrderEntity.builder()
                .market(mockOrderRequestDto.getMarket())
                .side(mockOrderRequestDto.getSide())
                .orderType(mockOrderRequestDto.getOrderType())
                .price(mockOrderRequestDto.getPrice())
                .volume(mockOrderRequestDto.getVolume())
                .executed(executed)
                .executedPrice(executedPrice)
                .build();

        mockOrderRepository.save(mockOrderEntity);

        MockOrderResponseDto responseDto = new MockOrderResponseDto(
                mockOrderEntity.getMarket(),
                mockOrderEntity.getSide(),
                mockOrderEntity.getOrderType(),
                mockOrderEntity.getPrice(),
                mockOrderEntity.getVolume(),
                mockOrderEntity.isExecuted(),
                mockOrderEntity.getExecutedPrice()
        );

        return ResponseEntity.ok().body(responseDto);
    }
}
