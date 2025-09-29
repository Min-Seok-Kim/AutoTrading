package org.example.autotrading;


import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AccountsService {
    private final UpbitAuthUtil upbitAuthUtil;

    private static final String BASE_URL = "https://api.upbit.com";
    private static final String PATH = "/v1/accounts";

    public ResponseEntity<?> selectAccounts() {
        String authHeader = upbitAuthUtil.createToken();

        System.out.println("ah: " + authHeader);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(BASE_URL + PATH)
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authHeader)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return ResponseEntity.ok().body(Objects.requireNonNull(response.body()).string());
        } catch (IOException e) {
            throw new RuntimeException("자산 조회 실패");
        }
    }
}
