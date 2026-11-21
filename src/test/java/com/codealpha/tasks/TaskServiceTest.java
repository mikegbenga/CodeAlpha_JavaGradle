package com.codealpha.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-21T12:00:00Z");
    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("create")
    class Create {
        @Test
        void assignsIncreasingIdsAndTimestamp() {
            Task a = service.create("Write build.gradle.kts");
            Task b = service.create("Add tests");
            assertEquals(1, a.id());
            assertEquals(2, b.id());
            assertEquals(NOW, a.createdAt());
            assertFalse(a.done());
        }

        @Test
        void trimsTitle() {
            assertEquals("Set up CI", service.create("   Set up CI  ").title());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t"})
        void rejectsBlankTitle(String title) {
            assertThrows(IllegalArgumentException.class, () -> service.create(title));
        }

        @Test
        void rejectsNullTitle() {
            assertThrows(IllegalArgumentException.class, () -> service.create(null));
        }

        @Test
        void rejectsVeryLongTitle() {
            assertThrows(IllegalArgumentException.class, () -> service.create("x".repeat(201)));
        }
    }

    @Test
    void listIsSortedById() {
        service.create("one");
        service.create("two");
        service.create("three");
        assertEquals(java.util.List.of(1L, 2L, 3L), service.list().stream().map(Task::id).toList());
    }

    @Test
    void completeMarksTaskDone() {
        Task t = service.create("Push to GitHub");
        assertTrue(service.complete(t.id()).orElseThrow().done());
        assertEquals(1, service.countDone());
    }

    @Test
    void completeUnknownIdIsEmpty() {
        assertTrue(service.complete(99).isEmpty());
    }

    @Test
    void deleteRemovesTask() {
        Task t = service.create("Delete me");
        assertTrue(service.delete(t.id()));
        assertFalse(service.delete(t.id()));
        assertTrue(service.find(t.id()).isEmpty());
    }

    @Test
    void defaultConstructorUsesSystemClock() {
        Task t = new TaskService().create("now");
        assertTrue(t.createdAt().isAfter(NOW.minusSeconds(60 * 60 * 24 * 365)));
    }
}
