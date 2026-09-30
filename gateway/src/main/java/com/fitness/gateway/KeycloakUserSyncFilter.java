package com.fitness.gateway;

import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.http.server.reactive.ServerHttpRequest; // <-- Isko lagao


import java.text.ParseException;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {
    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-USER-ID");
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");

        // Token null check taaki NullPointerException na aaye
        RegisterRequest registerRequest = (token != null) ? getUserDetails(token) : null;

        if (userId == null && registerRequest != null) {
            userId = registerRequest.getKeycloakId();
        }

        // Final variable banana zaroori hai lambda expressions mein use karne ke liye
        final String finalUserId = userId;

        if (finalUserId != null && token != null) {
            return userService.validateUser(finalUserId)
                    .flatMap(exists -> {
                        // Agar user database mein NAHI hai (exists == false), tab register karo
                        if (!exists && registerRequest != null) {
                            log.info("User does not exist in backend, registering now: {}", finalUserId);
                            return userService.registerUser(registerRequest).then(Mono.empty());
                        } else {
                            log.info("User already exists or register request is null, skipping registration.");
                            return Mono.empty();
                        }
                    })
                    .then(Mono.defer(() -> {
                        // Request ko mutate karke naya header set karna
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-ID", finalUserId)
                                .build();
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    }));
        }

        // Agar token ya userId nahi hai, to request ko bina roke seedhe aage jaane do
        return chain.filter(exchange);
    }

    private RegisterRequest getUserDetails(String token) {
        try {
            String tokenWithoutBearer = token.replace("Bearer", "").trim();
            SignedJWT signedJWT = SignedJWT.parse(tokenWithoutBearer);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            RegisterRequest request = new RegisterRequest();
            request.setEmail(claims.getStringClaim("email"));
            request.setKeycloakId(claims.getStringClaim("sub"));
            request.setFirstName(claims.getStringClaim("given_name"));
            request.setLastName(claims.getStringClaim("family_name"));
            request.setPassword("dummy@123123");

            return request;
        } catch (ParseException e) {
            log.error("Failed to parse JWT Token", e);
            throw new RuntimeException("Invalid token structure", e);
        }
    }
}
