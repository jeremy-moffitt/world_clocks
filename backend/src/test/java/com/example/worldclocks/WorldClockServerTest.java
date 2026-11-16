package com.example.worldclocks;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class WorldClockServerTest {

    /**
     * Verifies each selection has the configured size, contains no duplicates, and differs from the last.
     */
    @Test
    void selectsUniqueCitiesWithoutRepeatingThePreviousSelection() {
        List<WorldClockServer.City> firstSelection = WorldClockServer.chooseCities(null);
        List<WorldClockServer.City> nextSelection = WorldClockServer.chooseCities(firstSelection);
        Set<WorldClockServer.City> firstSet = new HashSet<>(firstSelection);

        assertEquals(8, firstSelection.size());
        assertEquals(firstSelection.size(), firstSet.size());
        assertEquals(8, nextSelection.size());
        assertEquals(nextSelection.size(), new HashSet<>(nextSelection).size());
        assertNotEquals(firstSet, new HashSet<>(nextSelection));
    }

    /**
     * Verifies JSON strings escape quotes, backslashes, and control characters.
     */
    @Test
    void escapesJsonStringContents() {
        assertEquals("\"line\\n\\t\\\"\\\\\\u0001\"",
                WorldClockServer.quote("line\n\t\"\\\u0001"));
    }
}
