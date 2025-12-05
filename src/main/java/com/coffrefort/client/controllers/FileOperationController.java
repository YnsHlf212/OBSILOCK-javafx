package com.coffrefort.client.controllers;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;

public class FileOperationController {
    private final ApiClient apiClient;
    private final TableView<FileEntry> table;
    private final Runnable refreshCallback;
    private final Consumer<String> statusCallback;

    private FileEntry clipboardFile;
    private boolean isCutOperation;

    public FileOperationController(ApiClient apiClient, TableView<FileEntry> table, 
                                   Runnable refreshCallback, Consumer<String> statusCallback) {
        this.apiClient = apiClient;
        this.table = table;
        this.refreshCallback = refreshCallback;
        this.statusCallback = statusCallback;
    }

    public void handleCopy() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            clipboardFile = selected;
            isCutOperation = false;
            showStatus("Fichier copié : " + selected.getName());
        }
    }

    public void handleCut() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            clipboardFile = selected;
            isCutOperation = true;
            showStatus("Fichier coupé : " + selected.getName());
        }
    }

    public void handlePaste(NodeItem currentFolder) {
        if (clipboardFile == null || currentFolder == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Coller");
        confirm.setHeaderText(isCutOperation ? "Déplacer le fichier" : "Copier le fichier");
        confirm.setContentText("Voulez-vous " + (isCutOperation ? "déplacer" : "copier") + 
                              " '" + clipboardFile.getName() + "' dans '" + currentFolder.getName() + "' ?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (isCutOperation) {
                showStatus("Fichier déplacé : " + clipboardFile.getName());
                clipboardFile = null;
            } else {
                showStatus("Fichier copié : " + clipboardFile.getName());
            }
            refreshCallback.run();
        }
    }

    public void handleRename() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Renommer");
        dialog.setHeaderText("Renommer le fichier");
        dialog.setContentText("Nouveau nom :");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                showStatus("Fichier renommé : " + selected.getName() + " → " + newName);
                refreshCallback.run();
            }
        });
    }

    public void handleDelete() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Voulez-vous vraiment supprimer '" + selected.getName() + "' ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteFileAsync(selected);
        }
    }

    private void deleteFileAsync(FileEntry file) {
        if (file.getId() == null) {
            showStatus("Erreur : ID du fichier introuvable");
            return;
        }

        javafx.concurrent.Task<Void> deleteTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                apiClient.deleteFile(file.getId());
                return null;
            }
        };

        deleteTask.setOnSucceeded(event -> {
            showStatus("Fichier supprimé : " + file.getName());
            refreshCallback.run();
        });

        deleteTask.setOnFailed(event -> {
            Throwable exception = deleteTask.getException();
            String errorMessage = "Erreur lors de la suppression du fichier";
            
            if (exception != null) {
                String exMsg = exception.getMessage();
                if (exMsg != null) {
                    if (exMsg.contains("404")) {
                        errorMessage = "Fichier introuvable.";
                    } else if (exMsg.contains("401")) {
                        errorMessage = "Session expirée. Veuillez vous reconnecter.";
                    } else if (exMsg.contains("403")) {
                        errorMessage = "Vous n'avez pas la permission de supprimer ce fichier.";
                    } else if (exMsg.contains("ConnectException")) {
                        errorMessage = "Impossible de contacter le serveur.";
                    } else {
                        errorMessage = "Erreur: " + exMsg;
                    }
                }
            }

            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Impossible de supprimer le fichier");
            error.setContentText(errorMessage);
            error.showAndWait();

            showStatus("Échec de la suppression : " + file.getName());
        });

        Thread thread = new Thread(deleteTask, "delete-file-task-thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void handleDownload() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le fichier");
        chooser.setInitialFileName(selected.getName());
        File file = chooser.showSaveDialog(table.getScene().getWindow());
        
        if (file != null) {
            showStatus("Téléchargement de : " + selected.getName());
        }
    }

    public void handleShare() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Créer un lien de partage");
        dialog.setHeaderText("Partager : " + selected.getName());

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        javafx.scene.control.TextField labelField = new javafx.scene.control.TextField("Mon partage");
        javafx.scene.control.TextField expiryField = new javafx.scene.control.TextField("7");
        javafx.scene.control.TextField maxUsesField = new javafx.scene.control.TextField("10");

        grid.add(new javafx.scene.control.Label("Nom du lien :"), 0, 0);
        grid.add(labelField, 1, 0);
        grid.add(new javafx.scene.control.Label("Expiration (jours) :"), 0, 1);
        grid.add(expiryField, 1, 1);
        grid.add(new javafx.scene.control.Label("Usages max :"), 0, 2);
        grid.add(maxUsesField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String shareUrl = "https://coffre.example.com/s/abc123xyz";
            
            ClipboardContent content = new ClipboardContent();
            content.putString(shareUrl);
            Clipboard.getSystemClipboard().setContent(content);
            
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Lien créé");
            info.setHeaderText("Lien de partage créé avec succès");
            info.setContentText("Le lien a été copié dans le presse-papiers :\n" + shareUrl);
            info.showAndWait();
        }
    }

    public void handleProperties() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Propriétés");
        info.setHeaderText("Propriétés du fichier");
        info.setContentText(
            "Nom : " + selected.getName() + "\n" +
            "Taille : " + humanSize(selected.getSize()) + " (" + selected.getSize() + " octets)\n" +
            "Modifié le : " + selected.getUpdatedAt().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );
        info.showAndWait();
    }

    public FileEntry getClipboardFile() {
        return clipboardFile;
    }

    public boolean isCutOperation() {
        return isCutOperation;
    }

    public void setClipboardFile(FileEntry file) {
        this.clipboardFile = file;
    }

    public void setClipboardFile(FileEntry file, boolean isCut) {
        this.clipboardFile = file;
        this.isCutOperation = isCut;
    }

    public void clearClipboard() {
        this.clipboardFile = null;
    }

    private void showStatus(String message) {
        statusCallback.accept(message);
    }

    private String humanSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#")
            .format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
