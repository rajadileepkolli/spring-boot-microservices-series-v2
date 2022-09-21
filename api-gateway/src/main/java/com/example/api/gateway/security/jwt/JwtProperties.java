/* Licensed under Apache-2.0 2021 */
package com.example.api.gateway.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secretKey;

    // validity in milliseconds
    private long validityInMs = 3_600_000; // 1h
}
