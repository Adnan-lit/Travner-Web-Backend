package org.adnan.travner.config;

import org.adnan.travner.controller.DebugController;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Debug endpoints must not be registered in production. */
class DebugProfileTest {

    @Test
    void debugControllerIsDisabledInProdProfile() {
        Profile profile = Arrays.stream(DebugController.class.getAnnotations())
                .filter(Profile.class::isInstance)
                .map(Profile.class::cast)
                .findFirst()
                .orElseThrow();

        assertTrue(Arrays.asList(profile.value()).contains("!prod"));
    }
}
