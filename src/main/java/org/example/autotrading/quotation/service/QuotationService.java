package org.example.autotrading.quotation.service;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.autotrading.accounts.service.AccountsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QuotationService {
    private final ObjectMapper objectMapper;
    private final AccountsService accountsService;

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
        ResponseEntity<?> currentPriceResponse = selectTicker(market);

        String body = (String) currentPriceResponse.getBody();

        List<Map<String, Object>> tickerList;
        try {
            tickerList = objectMapper.readValue(body, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("현재가 JSON 파싱 실패", e);
        }

        if (tickerList.isEmpty()) {
            throw new RuntimeException("현재가 조회 결과 없음");
        }

        double trade_price = Double.parseDouble(tickerList.get(0).get("trade_price").toString());

        ResponseEntity<?> accountResponse = accountsService.selectAccounts();
        String accountBody = (String) accountResponse.getBody();

        List<Map<String, Object>> accounts;

        try {
            accounts = objectMapper.readValue(accountBody, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("계좌 JSON 파싱 실패", e);
        }

        String currency = market.replace("KRW-", "");
        Map<String, Object> myCoin = accounts.stream()
                .filter(acc -> currency.equals(acc.get("currency")))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("해당 코인 없음"));

        double balance = Double.parseDouble(myCoin.get("balance").toString());
        double avgBuyPrice = Double.parseDouble(myCoin.get("avg_buy_price").toString());

        // 5. 손익 계산
        double evaluationAmount = balance * trade_price;       // 평가금액
        double investmentAmount = balance * avgBuyPrice;        // 매수금액
        double profitLoss = evaluationAmount - investmentAmount; // 손익
        double profitRate = (investmentAmount == 0) ? 0 : (profitLoss / investmentAmount) * 100;

        Map<String, Object> result = new HashMap<>();
        result.put("market", market);
        result.put("balance", balance);
        result.put("avg_buy_price", avgBuyPrice);
        result.put("current_price", trade_price);
        result.put("evaluation_amount", evaluationAmount);
        result.put("investment_amount", investmentAmount);
        result.put("profit_loss", profitLoss);
        result.put("profit_rate", profitRate);

        return ResponseEntity.ok().body(result);
    }
}
