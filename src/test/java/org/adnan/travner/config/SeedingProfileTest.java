package org.adnan.travner.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class SeedingProfileTest {

    @Test
    void demoSeederIsDisabledInProductionAndTests() {
        Profile profile = DataSeeder.class.getAnnotation(Profile.class);

        assertThat(profile).as("DataSeeder must be profile-gated").isNotNull();
        assertThat(profile.value()).containsExactly("!prod & !test");
    }
}
