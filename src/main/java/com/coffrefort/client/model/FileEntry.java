package com.coffrefort.client.model;

import java.time.Instant;

public class FileEntry {
    private final String name;
    private final long size;
    private final Instant updatedAt;
    private final int currentVersion;

    private FileEntry(String name, long size, Instant updatedAt, int currentVersion) {
        this.name = name;
        this.size = size;
        this.updatedAt = updatedAt;
        this.currentVersion = currentVersion;
    }

    public static FileEntry of(String name, long size, Instant updatedAt) {
        return new FileEntry(name, size, updatedAt, 1);
    }

    public static FileEntry of(String name, long size, Instant updatedAt, int currentVersion) {
        return new FileEntry(name, size, updatedAt, currentVersion);
    }

    public String getName() { return name; }
    public long getSize() { return size; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getCurrentVersion() { return currentVersion; }
}