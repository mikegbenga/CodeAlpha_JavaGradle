package com.codealpha.tasks;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory task store. Thread-safe because the HTTP server handles requests on several threads. */
public class TaskService {

    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final Clock clock;

    public TaskService() {
        this(Clock.systemUTC());
    }

    TaskService(Clock clock) {
        this.clock = clock;
    }

    public Task create(String title) {
        long id = nextId.getAndIncrement();
        Task task = new Task(id, title, false, clock.instant());
        tasks.put(id, task);
        return task;
    }

    public List<Task> list() {
        return tasks.values().stream()
                .sorted(Comparator.comparingLong(Task::id))
                .toList();
    }

    public Optional<Task> find(long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public Optional<Task> complete(long id) {
        return Optional.ofNullable(tasks.computeIfPresent(id, (k, t) -> t.markDone()));
    }

    public boolean delete(long id) {
        return tasks.remove(id) != null;
    }

    public long countDone() {
        return tasks.values().stream().filter(Task::done).count();
    }
}
