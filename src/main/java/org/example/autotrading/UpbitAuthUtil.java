package org.example.autotrading;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;


public class UpbitAuthUtil {
    @Value("${spring.upbit.access-key}")
    private String accessKey;
    @Value("${spring.upbit.secret-key}")
    private String secretKey;

    private static final String BASE_URL = "https://api.upbit.com";
    private static final String PATH = "/v1/accounts";

    public void createToken() {
        Algorithm algorithm = Algorithm.HMAC256(secretKey.getBytes(StandardCharsets.UTF_8));
        String jwtToken = JWT.create()
                .withClaim("accessKey", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .sign(algorithm);

        String authHeader = "Bearer " + jwtToken;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(BASE_URL + PATH)
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authHeader)
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println(response.code());
            System.out.println(Objects.requireNonNull(response.body()).string());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
