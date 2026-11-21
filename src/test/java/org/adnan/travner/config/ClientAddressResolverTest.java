package org.adnan.travner.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientAddressResolverTest {

    private final ClientAddressResolver resolver = new ClientAddressResolver("127.0.0.1,::1");

    @Test
    void trustsForwardedForOnlyFromConfiguredProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.8, 127.0.0.1");

        assertThat(resolver.resolveClientIp(request)).isEqualTo("198.51.100.8");
    }

    @Test
    void ignoresSpoofedForwardedForFromUntrustedRemote() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.8");

        assertThat(resolver.resolveClientIp(request)).isEqualTo("203.0.113.10");
    }
}
