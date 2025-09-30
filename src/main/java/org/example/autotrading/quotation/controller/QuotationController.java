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

    @GetMapping("/select/market/all")
    public ResponseEntity<?> selectMarket() {
        return quotationService.selectMarket();
    }

    @GetMapping("/select/ticker")
    public ResponseEntity<?> selectTicker(@RequestParam(name = "market") String market) {
        return quotationService.selectTicker(market);
    }

    @GetMapping("/select/overbook")
    public ResponseEntity<?> selectOverbook(@RequestParam(name = "market") String market) {
        return quotationService.selectOverbook(market);
    }
}
