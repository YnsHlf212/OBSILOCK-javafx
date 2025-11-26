package com.coffrefort.client.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un dossier (noeud) contenant éventuellement des fichiers et des sous-dossiers.
 */
public class NodeItem {
    private final String name;
    private final List<NodeItem> children = new ArrayList<>();
    private final List<FileEntry> files = new ArrayList<>();

    private NodeItem(String name) {
        this.name = name;
    }

    public static NodeItem folder(String name) {
        return new NodeItem(name);
    }

    public NodeItem addChild(NodeItem child) {
        this.children.add(child);
        return this;
    }

    public NodeItem withFiles(List<FileEntry> list) {
        this.files.clear();
        this.files.addAll(list);
        return this;
    }

    public String getName() { return name; }
    public List<NodeItem> getChildren() { return children; }
    public List<FileEntry> getFiles() { return files; }

    @Override
    public String toString() {
        return name;
    }
}
