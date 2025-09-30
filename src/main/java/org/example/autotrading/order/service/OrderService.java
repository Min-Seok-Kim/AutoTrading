package org.example.autotrading.order.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.autotrading.order.dto.MockOrderDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderService {

    public ResponseEntity<?> mockOrder(MockOrderDto mockOrderDto) throws IOException {
        OkHttpClient client = new OkHttpClient();

        String url = "https://api.upbit.com/v1/ticker?markets=" + mockOrderDto.getMarket();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = Objects.requireNonNull(response.body()).string();

            // JSON 파싱 (예: [{"market":"KRW-BTC","trade_price":...}] 형식)
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(body);

            double currentPrice = node.get(0).get("trade_price").asDouble();
            boolean executed;
            double executedPrice;

        } catch (IOException e) {
            throw new RuntimeException("Ticker 조회 실패", e);
        }
    }
}
