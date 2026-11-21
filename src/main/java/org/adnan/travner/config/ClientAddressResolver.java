package org.adnan.travner.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the effective client IP while trusting X-Forwarded-For only from
 * explicitly configured reverse proxies.
 */
@Component
public class ClientAddressResolver {

    private final Set<String> trustedProxyAddresses;

    public ClientAddressResolver(@Value("${app.security.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1,::1}") String trustedProxies) {
        this.trustedProxyAddresses = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxyAddresses.contains(remoteAddr)) {
            return remoteAddr;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddr;
        }

        String firstHop = forwardedFor.split(",")[0].trim();
        return firstHop.isBlank() ? remoteAddr : firstHop;
    }
}
