package org.adnan.travner.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code String} path variable that must be a valid MongoDB ObjectId.
 * The {@link ValidObjectIdArgumentResolver} validates it before the controller
 * method runs, so malformed IDs become HTTP 400 instead of leaking into a
 * service and surfacing as a 500. The {@code value} is the path-variable name.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidObjectId {
    String value();
}
