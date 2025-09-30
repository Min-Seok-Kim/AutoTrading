package org.example.autotrading.Accounts.Controller;


import lombok.RequiredArgsConstructor;
import org.example.autotrading.Accounts.AccountsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountsController {
    private final AccountsService accountsService;

    @GetMapping("/select/accounts")
    public ResponseEntity<?> select() {
        return accountsService.selectAccounts();
    }
}
