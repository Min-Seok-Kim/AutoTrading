package org.example.autotrading.order.service;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.example.autotrading.config.UpbitAuthUtil;
import org.example.autotrading.order.dto.MockOrderRequestDto;
import org.example.autotrading.order.dto.MockOrderResponseDto;
import org.example.autotrading.order.entity.MockOrderEntity;
import org.example.autotrading.order.repository.MockOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final ObjectMapper objectMapper;
    private final MockOrderRepository mockOrderRepository;

    @Value("${spring.upbit.access-key}")
    private String accessKey;
    @Value("${spring.upbit.secret-key}")
    private String secretKey;
    private static final String BASE_URL = "https://api.upbit.com";

    public ResponseEntity<?> mockOrder(MockOrderRequestDto mockOrderRequestDto) throws IOException {
        OkHttpClient client = new OkHttpClient();

        String url = BASE_URL + "/v1/ticker?markets=" + mockOrderRequestDto.getMarket();

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
            executedPrice = currentPrice; // 단가 그대로
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

        BigDecimal totalPrice = executed
                ? executedPrice.multiply(mockOrderRequestDto.getVolume())
                : BigDecimal.ZERO;

        MockOrderResponseDto responseDto = new MockOrderResponseDto(
                mockOrderEntity.getMarket(),
                mockOrderEntity.getSide(),
                mockOrderEntity.getOrderType(),
                mockOrderEntity.getPrice(),
                mockOrderEntity.getVolume(),
                mockOrderEntity.isExecuted(),
                mockOrderEntity.getExecutedPrice(),
                totalPrice
        );

        return ResponseEntity.ok().body(responseDto);
    }

    public ResponseEntity<?> sellRvn(BigDecimal volume) throws NoSuchAlgorithmException {
        OkHttpClient client = new OkHttpClient();

        Map<String, String> params = new HashMap<>();
        params.put("market", "KRW-BTC");
        params.put("side", "ask");
        params.put("volume", String.valueOf(volume));
        params.put("ord_type", "market");

        String queryString = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + String.valueOf(e.getValue()))
                .collect(Collectors.joining("&"));

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(queryString.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        String queryHash = sb.toString();

        Algorithm algorithm = Algorithm.HMAC512(secretKey.getBytes(StandardCharsets.UTF_8));
        String jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("query_hash", queryHash)
                .withClaim("query_hash_alg", "SHA512")
                .sign(algorithm);

        String authHeader = "Bearer " + jwtToken;

        String jsonBody = new Gson().toJson(params);

        Request request = new Request.Builder()
                .url(BASE_URL + "/v1/orders")
                .post(RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json; charset=utf-8")))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authHeader)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String resBody = Objects.requireNonNull(response.body()).string();
            return ResponseEntity.ok().body(resBody);
        } catch (IOException e) {
            throw new RuntimeException("BTC 시장가 매도 실패", e);
        }
    }

    public ResponseEntity<?> buyRvn(int price) throws NoSuchAlgorithmException {
        Map<String, String> params = new HashMap<>();

        params.put("market", "KRW-BTC");
        params.put("side", "bid");
        params.put("ord_type", "price");
        params.put("price", String.valueOf(price));

        // Map<String, String> 형태의 params를 쿼리스트링으로 변환
        // entryset()은 Map의 키와 값 쌍을 Set<Map.Entry<K, V>> 형태로 반환
        // .stream()은 반환된 Set을 stream으로 변환
        // map(e -> e.getKey() + "=" + e.getValue())은 각 Map.Entry(key-value)쌍을 "key=value" 문자열로 변환 -> ex) "market = KRW-BTC"
        // .collect(Collectors.joining("&"))은 "&"를 구분자로 하여 모든 문자열을 하나로 합침 -> "market=BTC-KRW&count=10"
        // 촤종 결과 -> ex) String queryString = "market=BTC-KRW&count=10";
        String queryString = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(queryString.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }

        String queryHash = sb.toString();

        Algorithm algorithm = Algorithm.HMAC512(secretKey.getBytes(StandardCharsets.UTF_8));
        String jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("query_hash", queryHash)
                .withClaim("query_hash_alg", "SHA512")
                .sign(algorithm);

        String authHeader = "Bearer " + jwtToken;

        String jsonBody = new Gson().toJson(params);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(BASE_URL + "/v1/orders")
                .post(RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json; charset=utf-8")))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authHeader)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String resBody = Objects.requireNonNull(response.body()).string();
            return ResponseEntity.ok().body(resBody);
        } catch (IOException e) {
            throw new RuntimeException("BTC 시장가 매수 실패", e);
        }
    }
}
