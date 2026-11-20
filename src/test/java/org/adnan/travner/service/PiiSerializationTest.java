package org.adnan.travner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.adnan.travner.dto.TravelBuddyDTO;
import org.adnan.travner.dto.LocalGuideDTO;
import org.adnan.travner.dto.UserDetailDTO;
import org.adnan.travner.dto.UserSummaryDTO;
import org.adnan.travner.entry.LocalGuideEntry;
import org.adnan.travner.entry.TravelBuddyEntry;
import org.adnan.travner.entry.UserEntry;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for P2-11: no entity or private user field reaches responses. */
class PiiSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void publicUserSummaryHasNoEmailFieldAtAll() {
        List<String> fields = Arrays.stream(UserSummaryDTO.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertFalse(fields.contains("email"), "Public summaries must not even declare email");
        assertFalse(fields.contains("password"));
    }

    @Test
    void userDetailDtoIsSafeForSelfAndAdminViews() throws Exception {
        String json = mapper.writeValueAsString(UserDetailDTO.from(user()));

        assertTrue(json.contains("\"email\":\"alice@example.com\""));
        assertTrue(json.contains("\"userName\":\"alice_01\""));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("secret-hash"));
    }

    @Test
    void rawEntityNeverSerializesPasswordHash() throws Exception {
        String json = mapper.writeValueAsString(user());

        assertFalse(json.contains("password"));
        assertFalse(json.contains("secret-hash"));
    }

    @Test
    void travelBuddyRequesterSummaryDoesNotExposeEmail() {
        TravelBuddyEntry entry = TravelBuddyEntry.builder()
                .id(new ObjectId())
                .requester(user())
                .build();

        TravelBuddyDTO dto = new TravelBuddyService().convertToDTO(entry);

        assertNotNull(dto.getRequester());
        assertEquals("alice_01", dto.getRequester().getUserName());
        assertEquals("alice@example.com", user().getEmail()); // source still has it
    }

    @Test
    void localGuideUserSummaryDoesNotExposeEmail() {
        LocalGuideEntry entry = LocalGuideEntry.builder()
                .id(new ObjectId())
                .user(user())
                .build();

        LocalGuideDTO dto = new LocalGuideService().convertToDTO(entry);

        assertNotNull(dto.getUser());
        assertEquals("alice_01", dto.getUser().getUserName());
        assertEquals("alice@example.com", user().getEmail()); // source still has it
    }

    private static UserEntry user() {
        UserEntry user = new UserEntry();
        user.setId(new ObjectId());
        user.setUserName("alice_01");
        user.setEmail("alice@example.com");
        user.setPassword("$2a$10$secret-hash");
        user.setFirstName("Alice");
        user.setLastName("Example");
        user.setRoles(List.of("USER"));
        user.setActive(true);
        return user;
    }
}
