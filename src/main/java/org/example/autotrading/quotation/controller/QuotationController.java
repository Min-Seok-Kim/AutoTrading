package org.example.autotrading.quotation.controller;


import lombok.RequiredArgsConstructor;
import org.example.autotrading.quotation.service.QuotationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuotationController {
    private final QuotationService quotationService;

    /**
     * 코인 조회
     * @return
     */
    @GetMapping("/select/market/all")
    public ResponseEntity<?> selectMarket() {
        return quotationService.selectMarket();
    }

    /**
     * 현재가 조회
     * @param market
     * @return
     */
    @GetMapping("/select/ticker")
    public ResponseEntity<?> selectTicker(@RequestParam(name = "market") String market) {
        return quotationService.selectTicker(market);
    }

    /**
     * 호가 조회
     * @param market
     * @return
     */
    @GetMapping("/select/overbook")
    public ResponseEntity<?> selectOverbook(@RequestParam(name = "market") String market) {
        return quotationService.selectOverbook(market);
    }

    /**
     * 손익 계산
     * @param market
     * @return
     */
    @GetMapping("/select/profit-loss")
    public ResponseEntity<?> selectProfitLoss(@RequestParam String market) {return quotationService.selectProfitLoss(market);}
}
