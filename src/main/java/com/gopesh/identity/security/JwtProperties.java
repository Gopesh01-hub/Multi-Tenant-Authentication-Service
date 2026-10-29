package com.gopesh.identity.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "identity.jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiry = 900_000;
    private long refreshTokenExpiry = 604_800_000;
    private String issuer = "identity-platform-oss";
}
