package com.codealpha.tasks;

import java.time.Instant;

/** A to-do item. Records give us equals/hashCode/toString for free and are immutable. */
public record Task(long id, String title, boolean done, Instant createdAt) {

    public Task {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        title = title.strip();
        if (title.length() > 200) {
            throw new IllegalArgumentException("title must be 200 characters or fewer");
        }
    }

    public Task markDone() {
        return new Task(id, title, true, createdAt);
    }
}
