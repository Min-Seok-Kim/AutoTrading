package org.example.autotrading.quotation.service;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QuotationService {
    private final ObjectMapper objectMapper;

    public ResponseEntity<?> selectMarket() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.upbit.com/v1/market/all")
                .get()
                .addHeader("accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return ResponseEntity.ok().body(Objects.requireNonNull(response.body()).string());
        } catch (IOException e) {
            throw new RuntimeException("페어 목록 조회 실패");
        }
    }

    public ResponseEntity<?> selectTicker(String market) {
        OkHttpClient client = new OkHttpClient();

        String url = "https://api.upbit.com/v1/ticker?markets=" + market;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return ResponseEntity.ok().body(Objects.requireNonNull(response.body()).string());
        } catch (IOException e) {
            throw new RuntimeException("Type mismatch error. Check the parameters type!");
        }
    }

    public ResponseEntity<?> selectOverbook(String market) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.upbit.com/v1/orderbook?markets=" + market)
                .get()
                .addHeader("accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return ResponseEntity.ok().body(Objects.requireNonNull(response.body()).string());
        } catch (IOException e) {
            throw new RuntimeException( "Invalid parameter. Check the given value!");
        }
    }

    public ResponseEntity<?> selectProfitLoss(String market) {
        ResponseEntity<?> myAccount = selectTicker(market);
        String trade_price = (String) myAccount.getBody();

        List<Map<String, Object>> accounts;
        try {
            accounts = objectMapper.readValue(trade_price, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("계좌 JSON 파싱 실패", e);
        }
    }
}
