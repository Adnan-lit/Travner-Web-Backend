package org.adnan.travner.util;

import org.adnan.travner.entry.CartEntry;
import org.adnan.travner.entry.UserEntry;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IdentityNormalizerTest {

    @Test
    void trimsAndLowercasesWithRootLocale() {
        Locale original = Locale.getDefault();
        try {
            // Turkish-I would turn "I" into a dotless "ı" without ROOT.
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals("istanbul", IdentityNormalizer.normalizeUsername("  ISTANBUL  "));
            assertEquals("alice@example.com", IdentityNormalizer.normalizeEmail(" ALICE@Example.COM "));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void nullStaysNullAndBlankStaysBlank() {
        assertNull(IdentityNormalizer.normalizeUsername(null));
        assertNull(IdentityNormalizer.normalizeEmail(null));
        assertEquals("", IdentityNormalizer.normalizeUsername("   "));
        assertEquals("", IdentityNormalizer.normalizeEmail("\t"));
    }

    @Test
    void userEntryNormalizesOnSettersAndBuilder() {
        UserEntry entry = new UserEntry();
        entry.setUserName("  Alice_01  ");
        entry.setEmail("  ALICE@Example.com ");
        assertEquals("alice_01", entry.getUserName());
        assertEquals("alice@example.com", entry.getEmail());

        UserEntry built = UserEntry.builder()
                .userName("  Bob_02  ")
                .email(" BOB@Example.COM ")
                .build();
        assertEquals("bob_02", built.getUserName());
        assertEquals("bob@example.com", built.getEmail());
    }

    @Test
    void cartEntryNormalizesItsUsernameKey() {
        CartEntry entry = CartEntry.builder().userId("  Alice  ").build();
        assertEquals("alice", entry.getUserId());

        entry.setUserId(" BOB ");
        assertEquals("bob", entry.getUserId());
    }
}
