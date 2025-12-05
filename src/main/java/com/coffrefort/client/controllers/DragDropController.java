package com.coffrefort.client.controllers;

import java.util.Optional;

import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class DragDropController {
    private final TableView<FileEntry> table;
    private final TreeView<NodeItem> treeView;
    private final Runnable refreshCallback;
    private final FileOperationController fileOpController;

    public DragDropController(TableView<FileEntry> table, TreeView<NodeItem> treeView, 
                             Runnable refreshCallback, FileOperationController fileOpController) {
        this.table = table;
        this.treeView = treeView;
        this.refreshCallback = refreshCallback;
        this.fileOpController = fileOpController;
    }

    public void setupDragAndDrop() {
        setupTableDragSource();
        setupTreeDragTarget();
        setupTableDragTarget();
    }

    private void setupTableDragSource() {
        if (table == null) return;

        table.setOnDragDetected(event -> {
            FileEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = table.startDragAndDrop(TransferMode.MOVE);
                
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(selected.getName());
                db.setContent(content);
                
                fileOpController.setClipboardFile(selected, true);
                
                event.consume();
            }
        });

        table.setOnDragDone(event -> {
            if (event.getTransferMode() == TransferMode.MOVE) {
            }
            event.consume();
        });
    }

    private void setupTreeDragTarget() {
        if (treeView == null) return;

        treeView.setOnDragOver(event -> {
            if (event.getGestureSource() != treeView && 
                event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        treeView.setOnDragEntered(event -> {
            if (event.getGestureSource() != treeView && 
                event.getDragboard().hasString()) {
                treeView.setStyle("-fx-border-color: #0078d4; -fx-border-width: 2px;");
            }
            event.consume();
        });

        treeView.setOnDragExited(event -> {
            treeView.setStyle("");
            event.consume();
        });

        treeView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            FileEntry clipboardFile = fileOpController.getClipboardFile();
            
            if (db.hasString() && clipboardFile != null) {
                javafx.scene.control.TreeItem<NodeItem> targetItem = treeView.getSelectionModel().getSelectedItem();
                if (targetItem != null) {
                    NodeItem targetFolder = targetItem.getValue();
                    
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Déplacer le fichier");
                    confirm.setHeaderText("Confirmer le déplacement");
                    confirm.setContentText("Déplacer '" + clipboardFile.getName() + 
                                          "' vers '" + targetFolder.getName() + "' ?");
                    
                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        System.out.println("Fichier déplacé vers : " + targetFolder.getName());
                        
                        refreshCallback.run();
                        success = true;
                    }
                }
                
                fileOpController.clearClipboard();
            }
            
            event.setDropCompleted(success);
            treeView.setStyle("");
            event.consume();
        });
    }

    private void setupTableDragTarget() {
        if (table == null) return;

        table.setOnDragOver(event -> {
            if (event.getGestureSource() == table && 
                event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        table.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            FileEntry clipboardFile = fileOpController.getClipboardFile();
            
            if (db.hasString() && clipboardFile != null) {
                System.out.println("Réorganisation dans le dossier actuel");
                success = true;
                fileOpController.clearClipboard();
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }
}
