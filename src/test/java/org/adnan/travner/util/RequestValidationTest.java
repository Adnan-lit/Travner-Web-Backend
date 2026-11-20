package org.adnan.travner.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestValidationTest {

    private static final Set<String> ALLOWED = Set.of("createdAt", "title");

    @Test
    void negativePageIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> RequestValidation.page(-1));
    }

    @Test
    void zeroPageIsAllowed() {
        assertEquals(0, RequestValidation.page(0));
    }

    @Test
    void zeroAndNegativeSizesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RequestValidation.size(0));
        assertThrows(IllegalArgumentException.class, () -> RequestValidation.size(-5));
    }

    @Test
    void oversizedPageIsCappedAtOneHundred() {
        assertEquals(100, RequestValidation.size(101));
        assertEquals(100, RequestValidation.size(10_000));
        assertEquals(25, RequestValidation.size(25));
    }

    @Test
    void directionIsCaseInsensitiveAndDefaultsToDesc() {
        assertEquals(Sort.Direction.ASC, RequestValidation.direction("asc"));
        assertEquals(Sort.Direction.ASC, RequestValidation.direction("ASC"));
        assertEquals(Sort.Direction.DESC, RequestValidation.direction("desc"));
        assertEquals(Sort.Direction.DESC, RequestValidation.direction(null));
    }

    @Test
    void invalidDirectionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> RequestValidation.direction("sideways"));
    }

    @Test
    void unknownSortFieldIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> RequestValidation.sortBy("password", ALLOWED, "createdAt"));
    }

    @Test
    void blankSortFieldFallsBackToDefault() {
        assertEquals("createdAt", RequestValidation.sortBy(null, ALLOWED, "createdAt"));
        assertEquals("createdAt", RequestValidation.sortBy("  ", ALLOWED, "createdAt"));
    }

    @Test
    void pageableCombinesValidationRules() {
        Pageable pageable = RequestValidation.pageable(2, 500, "title", "asc", ALLOWED, "createdAt");

        assertEquals(2, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "title"), pageable.getSort());
    }

    @Test
    void unsortedPageableStillValidatesPageAndSize() {
        Pageable pageable = RequestValidation.pageable(0, 200);

        assertEquals(100, pageable.getPageSize());
        assertThrows(IllegalArgumentException.class, () -> RequestValidation.pageable(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> RequestValidation.pageable(0, 0));
    }
}
