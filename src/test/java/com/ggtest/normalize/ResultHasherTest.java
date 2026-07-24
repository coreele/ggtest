package com.ggtest.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResultHasherTest {

    @Test
    void md5MatchesSelect1CorpusSample() {
        // Excerpt: select1.test first query — 30 I values hashing to known MD5
        List<String> values = List.of(
                "358", "364", "376", "382", "398", "402", "410", "426", "432", "440",
                "458", "468", "478", "486", "490", "1000", "1050", "1120", "1180", "1240",
                "1290", "1300", "1390", "1430", "1450", "1510", "1580", "1600", "1670", "1700");
        assertEquals("3c13dee48d9356ae19af2515e05e6b54", ResultHasher.md5Hex(values));
        assertEquals(
                "30 values hashing to 3c13dee48d9356ae19af2515e05e6b54",
                ResultHasher.hashForm(values));
    }

    @Test
    void parsesExpectedHashLine() {
        Optional<ResultHasher.HashExpectation> parsed =
                ResultHasher.parseHashExpectation("30 values hashing to 3c13dee48d9356ae19af2515e05e6b54");
        assertTrue(parsed.isPresent());
        assertEquals(30, parsed.get().valueCount());
        assertEquals("3c13dee48d9356ae19af2515e05e6b54", parsed.get().md5Hex());
    }

    @Test
    void rejectsNonHashExpectedText() {
        assertFalse(ResultHasher.parseHashExpectation("1\n2\n3").isPresent());
        assertFalse(ResultHasher.parseHashExpectation("").isPresent());
    }
}
