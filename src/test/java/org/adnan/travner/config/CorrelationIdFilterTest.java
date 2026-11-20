package org.adnan.travner.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesIdWhenHeaderAbsent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] mdcDuringChain = new String[1];

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                mdcDuringChain[0] = MDC.get(CorrelationIdFilter.MDC_KEY);
            }
        });

        String id = response.getHeader(CorrelationIdFilter.HEADER);
        assertTrue(id.matches("[0-9a-f-]{36}"), "expected a UUID, got: " + id);
        assertEquals(id, mdcDuringChain[0]);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY), "MDC must be cleaned up after the request");
    }

    @Test
    void acceptsSafeCallerSuppliedId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "client-trace_01");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("client-trace_01", response.getHeader(CorrelationIdFilter.HEADER));
    }

    @Test
    void rejectsUnsafeCallerSuppliedId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "bad\nheader\r\ninjection:<script>");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String id = response.getHeader(CorrelationIdFilter.HEADER);
        assertTrue(id.matches("[0-9a-f-]{36}"));
    }
}
