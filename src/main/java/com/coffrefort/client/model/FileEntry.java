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
    private final Integer id;
    private final String name;
    private final long size;
    private final Instant updatedAt;
    private final int currentVersion;

    private FileEntry(Integer id, String name, long size, Instant updatedAt, int currentVersion) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.size = size;
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
        this.currentVersion = currentVersion;
    }

    public static FileEntry of(String name, long size, Instant updatedAt, int currentVersion) {
        return new FileEntry(null, name, size, updatedAt, currentVersion);
    }

    public static FileEntry of(Integer id, String name, long size, Instant updatedAt, int currentVersion) {
        return new FileEntry(id, name, size, updatedAt, currentVersion);
    }

    public Integer getId() { return id; }
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
                Objects.equals(id, fileEntry.id) &&
                Objects.equals(name, fileEntry.name) &&
                Objects.equals(updatedAt, fileEntry.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, size, updatedAt, currentVersion);
    }
}