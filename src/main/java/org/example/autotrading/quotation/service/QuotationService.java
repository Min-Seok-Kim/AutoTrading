package org.example.autotrading.quotation.service;



import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.autotrading.accounts.service.AccountsService;
import org.example.autotrading.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationService {
    private final ObjectMapper objectMapper;
    private final AccountsService accountsService;
    private final OrderService orderService;
    private BigDecimal previousPrice = BigDecimal.ZERO;
    private BigDecimal balance = BigDecimal.ZERO;
    private final BigDecimal MIN_ORDER_KRW = new BigDecimal("5000");
    private final OkHttpClient client = new OkHttpClient();

    private final Queue<BigDecimal> recentPrices = new LinkedList<>();
    private static final int MOVING_AVERAGE_PERIOD = 20;
    private boolean hasCoin = false;

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

            if(recentPrices.size() >= MOVING_AVERAGE_PERIOD) recentPrices.poll();

            recentPrices.offer(currentPrice);

            if (recentPrices.size() == MOVING_AVERAGE_PERIOD) {
                BigDecimal movingAverage = calculateMovingAverage();
                log.info("📊 이동평균선(20틱): {}", movingAverage.toPlainString());

                // 매수 조건
                if (currentPrice.compareTo(movingAverage) < 0 && !hasCoin) {
                    log.info("📉 매수 신호 감지 (현재가 < 이동평균선)");

                    // 실제 체결 수량 반환
                    ResponseEntity<?> buyResponse = orderService.buyRvn(5000);
                    String body = (String) buyResponse.getBody();

                    Map<String, Object> buyResult = objectMapper.readValue(body, new TypeReference<>(){});



                    BigDecimal executedCoin = new BigDecimal(
                            Optional.ofNullable(buyResult.get("executed_volume"))
                                    .map(Object::toString)
                                    .orElse("0")
                    );
                    BigDecimal lockedAmount = new BigDecimal(
                            Optional.ofNullable(buyResult.get("locked"))
                                    .map(Object::toString)
                                    .orElse("0")
                    );
                    if (lockedAmount.compareTo(BigDecimal.ZERO) > 0) {
                        balance = executedCoin.divide(currentPrice, 8, RoundingMode.HALF_UP);
                        hasCoin = true;
                        log.info("✅ {} 원어치 매수 완료, 코인 수량: {}", lockedAmount, balance);
                    } else {
                        log.warn("매수 체결 금액이 0입니다. 응답: {}", buyResult);
                    }
                }

                // 매도 조건
                else if (currentPrice.compareTo(movingAverage) > 0 && hasCoin) {
                    log.info("📈 매도 신호 감지 (현재가 > 이동평균선)");

                    ResponseEntity<?> sellResponse = orderService.sellRvn(balance);
                    String body = (String) sellResponse.getBody();

                    Map<String, Object> sellResult = objectMapper.readValue(body, new TypeReference<>(){});

                    BigDecimal executedCoin = new BigDecimal(
                            Optional.ofNullable(sellResult.get("executed_volume"))
                                    .map(Object::toString)
                                    .orElse("0")
                    );
                    BigDecimal tradePrice = new BigDecimal(
                            Optional.ofNullable(sellResult.get("price"))
                                    .map(Object::toString)
                                    .orElse("0")
                    );

                    BigDecimal soldAmount = executedCoin.multiply(tradePrice);
                    if (soldAmount.compareTo(BigDecimal.ZERO) > 0) {
                        log.info("✅ {} 코인 매도 완료, 매도 금액: {}", balance, soldAmount);
                        balance = BigDecimal.ZERO;
                        hasCoin = false;
                    } else {
                        log.warn("매도 체결 금액이 0입니다. 응답: {}", sellResult);
                    }
                }
            }
        } catch (Exception e) {
            log.error("자동매매 실행 중 오류 발생: {}", e.getMessage());
        }
    }

    private BigDecimal calculateMovingAverage() {
        return recentPrices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(MOVING_AVERAGE_PERIOD), RoundingMode.HALF_UP);
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
