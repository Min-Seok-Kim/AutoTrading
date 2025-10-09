package org.example.autotrading.quotation.service;



import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.autotrading.accounts.service.AccountsService;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationService {
    private final ObjectMapper objectMapper;
    private final AccountsService accountsService;
    private BigDecimal previousPrice = BigDecimal.ZERO;
    private final OkHttpClient client = new OkHttpClient();

    /**
     * 코인 조회
     * @return
     */
    public ResponseEntity<?> selectMarket() {
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

    /**
     * 현재가 조회
     * @return
     */
    public ResponseEntity<?> selectTicker(String market) {
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

    /**
     * 호가 조회
     * @return
     */
    public ResponseEntity<?> selectOverbook(String market) {
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

    /**
     * 손익 조회
     * @return
     */
    public ResponseEntity<?> selectProfitLoss(String market) {
        // 현재가 응답
        ResponseEntity<?> currentPriceResponse = selectTicker(market);

        // 현재가 body
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

        String currency = market.replace("KRW-", ""); // RVN
        Map<String, Object> myCoin = accounts.stream()
                .filter(acc -> currency.equals(acc.get("currency")))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("해당 코인 없음"));

        // 주문 가능 수량 또는 금액.
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

    /**
     * 자동 현재가 조회
     *
     * @return
     */
    @Scheduled(fixedRate=5000)
    public void autoSelectTicker() {
        try {
            BigDecimal currentPrice = getCurrentPrice();

            log.info("현재가: {}", currentPrice.toPlainString());

            if(previousPrice.compareTo(BigDecimal.ZERO) > 0) {
                if(currentPrice.compareTo(previousPrice) < 0) {
                    log.info("📉 매수 기회 감지 (이전: {}, 현재: {})", previousPrice.toPlainString(), currentPrice.toPlainString());
                } else {
                    log.info("📈 상승 중 (이전: {}, 현재: {})", previousPrice.toPlainString(), currentPrice.toPlainString());
                }
            }

            previousPrice = currentPrice;
        } catch (Exception e) {
            log.error("가격 조회 실패: {}", e.getMessage());
        }
    }

    private BigDecimal getCurrentPrice() throws IOException {
        String url = "https://api.upbit.com/v1/ticker?markets=" + "KRW-BTC";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = Objects.requireNonNull(response.body()).string();
            List<Map<String, Object>> tickerList = objectMapper.readValue(body, new TypeReference<>() {});
            return new BigDecimal(tickerList.get(0).get("trade_price").toString());
        }
    }
}
