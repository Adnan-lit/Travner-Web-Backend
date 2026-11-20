package org.adnan.travner.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidObjectIdArgumentResolverTest {

    private final ValidObjectIdArgumentResolver resolver = new ValidObjectIdArgumentResolver();

    @SuppressWarnings("unused")
    void annotated(@ValidObjectId("id") String id) {
    }

    @SuppressWarnings("unused")
    void plain(String id) {
    }

    private MethodParameter parameter(String methodName) throws Exception {
        Method method = getClass().getDeclaredMethod(methodName, String.class);
        return new MethodParameter(method, 0);
    }

    private NativeWebRequest requestWith(String id) {
        NativeWebRequest request = mock(NativeWebRequest.class);
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST)).thenReturn(Map.of("id", id));
        return request;
    }

    @Test
    void supportsOnlyAnnotatedStringParameters() throws Exception {
        assertTrue(resolver.supportsParameter(parameter("annotated")));
        assertFalse(resolver.supportsParameter(parameter("plain")));
    }

    @Test
    void resolvesValidIdToCanonicalHex() throws Exception {
        assertEquals("507f191e810c19729de860ea",
                resolver.resolveArgument(parameter("annotated"), null,
                        requestWith("507F191E810C19729DE860EA"), null));
    }

    @Test
    void rejectsMalformedIdBeforeControllerBody() {
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolveArgument(parameter("annotated"), null, requestWith("not-an-id"), null));
    }

    @Test
    void rejectsMissingId() {
        NativeWebRequest request = mock(NativeWebRequest.class);
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST)).thenReturn(Map.of("other", "value"));

        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolveArgument(parameter("annotated"), null, request, null));
    }
}
