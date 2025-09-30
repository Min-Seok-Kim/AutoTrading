package org.example.autotrading.Config;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class UpbitAuthUtil {
    @Value("${spring.upbit.access-key}")
    private String accessKey;
    @Value("${spring.upbit.secret-key}")
    private String secretKey;



    public String createToken() {
        System.out.println("ac: " + accessKey);
        System.out.println("sc: " + secretKey);
        Algorithm algorithm = Algorithm.HMAC256(secretKey.getBytes(StandardCharsets.UTF_8));
        String jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .sign(algorithm);

        return "Bearer " + jwtToken;
    }
}
