package org.adnan.travner.web;

import org.adnan.travner.util.ObjectIdUtil;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * Resolves {@link ValidObjectId} parameters from URI template variables and
 * rejects malformed values. Because this is a custom annotation, built-in
 * resolvers do not claim the parameter, and validation happens before the
 * controller body (and therefore before any broad {@code catch (Exception)}).
 */
public class ValidObjectIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ValidObjectId.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String name = parameter.getParameterAnnotation(ValidObjectId.class).value();
        Object variables = webRequest.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        String value = variables instanceof Map<?, ?> map ? (String) map.get(name) : null;
        // Throws IllegalArgumentException -> 400 via GlobalExceptionHandler.
        return ObjectIdUtil.safeObjectId(value).toHexString();
    }
}
