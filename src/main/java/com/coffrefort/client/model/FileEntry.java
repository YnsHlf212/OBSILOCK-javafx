package com.coffrefort.client.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Représente une entrée de fichier (métadonnées) utilisée par l'UI et l'API.
 *
 * Champs minimaux attendus par le code existant :
 * - `name`
 * - `size` (octets)
 * - `updatedAt` (Instant)
 * - `currentVersion` (int)
 */
public class FileEntry {
    private final String name;
    private final long size;
    private final Instant updatedAt;
    private final int currentVersion;

    private FileEntry(String name, long size, Instant updatedAt, int currentVersion) {
        this.name = name == null ? "" : name;
        this.size = size;
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
        this.currentVersion = currentVersion;
    }

    public static FileEntry of(String name, long size, Instant updatedAt, int currentVersion) {
        return new FileEntry(name, size, updatedAt, currentVersion);
    }

    public String getName() { return name; }
    public long getSize() { return size; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getCurrentVersion() { return currentVersion; }

    @Override
    public String toString() {
        return "FileEntry{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", updatedAt=" + updatedAt +
                ", currentVersion=" + currentVersion +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEntry fileEntry = (FileEntry) o;
        return size == fileEntry.size &&
                currentVersion == fileEntry.currentVersion &&
                Objects.equals(name, fileEntry.name) &&
                Objects.equals(updatedAt, fileEntry.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, size, updatedAt, currentVersion);
    }
}