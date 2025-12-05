package com.coffrefort.client.controllers;

import java.text.DecimalFormat;
import java.util.Optional;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.NodeItem;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class FolderOperationController {
    private final ApiClient apiClient;
    private final TreeView<NodeItem> treeView;
    private final Runnable refreshCallback;

    public FolderOperationController(ApiClient apiClient, TreeView<NodeItem> treeView, 
                                     Runnable refreshCallback) {
        this.apiClient = apiClient;
        this.treeView = treeView;
        this.refreshCallback = refreshCallback;
    }

    public void handleCreateFolder(NodeItem currentFolder) {
        createNewFolder(currentFolder);
    }

    private void createNewFolder(NodeItem currentFolder) {
        System.out.println("\n=== createNewFolder() appelée ===");
        System.out.println("currentFolder: " + (currentFolder != null ? currentFolder.getName() : "null"));
        System.out.println("currentFolder ID: " + (currentFolder != null ? currentFolder.getId() : "null"));
        System.out.println("apiClient: " + (apiClient != null ? "initialized" : "null"));
        
        TextInputDialog dialog = new TextInputDialog("Nouveau dossier");
        dialog.setTitle("Créer un dossier");
        dialog.setHeaderText("Créer un nouveau dossier" + 
            (currentFolder != null ? " dans '" + currentFolder.getName() + "'" : ""));
        dialog.setContentText("Nom du dossier :");

        dialog.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (okButton != null) {
                boolean isValid = newVal != null && !newVal.trim().isEmpty() 
                    && !newVal.contains("/") && !newVal.contains("\\")
                    && !newVal.contains(":") && !newVal.contains("*")
                    && !newVal.contains("?") && !newVal.contains("\"")
                    && !newVal.contains("<") && !newVal.contains(">")
                    && !newVal.contains("|");
                okButton.setDisable(!isValid);
            }
        });

        Optional<String> result = dialog.showAndWait();
        System.out.println("Dialog result: " + result.isPresent());
        
        result.ifPresent(folderName -> {
            String trimmedName = folderName.trim();
            System.out.println("Nom du dossier saisi: " + trimmedName);
            
            if (!trimmedName.isEmpty()) {
                System.out.println("Appel de createFolderAsync()...");
                createFolderAsync(trimmedName, currentFolder);
            } else {
                System.out.println("Nom vide, annulation");
            }
        });
    }

    private void createFolderAsync(String folderName, NodeItem currentFolder) {
        System.out.println("\n=== createFolderAsync() appelée ===");
        System.out.println("Nom du dossier: " + folderName);
        System.out.println("Parent ID: " + (currentFolder != null ? currentFolder.getId() : "null"));
        
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Création en cours");
        progressAlert.setHeaderText("Création du dossier : " + folderName);
        progressAlert.setContentText("Veuillez patienter...");
        
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressAlert.setGraphic(progressIndicator);
        
        progressAlert.getButtonTypes().clear();
        progressAlert.show();
        System.out.println("Dialog de progression affiché");
        
        Integer parentId = currentFolder != null ? currentFolder.getId() : null;
        
        javafx.concurrent.Task<Integer> createTask = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() throws Exception {
                System.out.println("\n=== Task.call() exécutée ===");
                System.out.println("Thread: " + Thread.currentThread().getName());
                System.out.println("Appel apiClient.createFolder(\"" + folderName + "\", " + parentId + ")");
                
                try {
                    Integer result = apiClient.createFolder(folderName, parentId);
                    System.out.println("Résultat: " + result);
                    return result;
                } catch (Exception e) {
                    System.err.println("Exception dans Task.call(): " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }
        };
        
        createTask.setOnSucceeded(event -> {
            System.out.println("\n=== Task SUCCEEDED ===");
            
            javafx.application.Platform.runLater(() -> {
                try {
                    System.out.println("Fermeture du dialogue de progression...");
                    progressAlert.close();
                    System.out.println("Dialogue fermé");
                    
                    Integer folderId = createTask.getValue();
                    System.out.println("ID du dossier créé: " + folderId);
                    
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Dossier créé");
                    success.setHeaderText("Dossier créé avec succès");
                    success.setContentText("Le dossier '" + folderName + "' a été créé.\n" +
                                        (folderId != null ? "ID: " + folderId : ""));
                    success.showAndWait();
                    
                    System.out.println("Rafraîchissement de l'affichage...");
                    refreshCallback.run();
                } catch (Exception e) {
                    System.err.println("Erreur dans Platform.runLater (success): " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
        
        createTask.setOnFailed(event -> {
            System.err.println("\n=== Task FAILED ===");
            
            javafx.application.Platform.runLater(() -> {
                try {
                    System.out.println("Fermeture du dialogue de progression (erreur)...");
                    progressAlert.close();
                    System.out.println("Dialogue fermé");
                    
                    Throwable exception = createTask.getException();
                    System.err.println("Exception: " + (exception != null ? exception.getClass().getName() : "null"));
                    System.err.println("Message: " + (exception != null ? exception.getMessage() : "null"));
                    
                    String errorMessage = "Erreur lors de la création du dossier";
                    
                    if (exception != null) {
                        exception.printStackTrace();
                        String exMsg = exception.getMessage();
                        
                        if (exMsg != null) {
                            if (exMsg.contains("409")) {
                                errorMessage = "Un dossier avec ce nom existe déjà.";
                            } else if (exMsg.contains("400")) {
                                errorMessage = "Nom de dossier invalide.";
                            } else if (exMsg.contains("401")) {
                                errorMessage = "Session expirée. Veuillez vous reconnecter.";
                            } else if (exMsg.contains("404")) {
                                errorMessage = "Endpoint introuvable. Vérifiez l'URL du serveur.";
                            } else if (exMsg.contains("500")) {
                                errorMessage = "Erreur interne du serveur.";
                            } else if (exMsg.contains("ConnectException") || exMsg.contains("Connection refused")) {
                                errorMessage = "Impossible de contacter le serveur.\nVérifiez que le backend est démarré.";
                            } else if (exMsg.contains("timeout")) {
                                errorMessage = "Délai d'attente dépassé.\nLe serveur ne répond pas.";
                            } else {
                                errorMessage = "Erreur: " + exMsg;
                            }
                        }
                    }
                    
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Erreur");
                    error.setHeaderText("Impossible de créer le dossier");
                    error.setContentText(errorMessage);
                    error.showAndWait();
                } catch (Exception e) {
                    System.err.println("Erreur dans Platform.runLater (error): " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
        
        Thread thread = new Thread(createTask, "create-folder-task-thread");
        thread.setDaemon(true);
        System.out.println("Démarrage du thread...");
        thread.start();
        System.out.println("Thread démarré");
    }

    public void handleRenameFolder() {
        TreeItem<NodeItem> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;

        NodeItem folder = selected.getValue();
        
        TextInputDialog dialog = new TextInputDialog(folder.getName());
        dialog.setTitle("Renommer le dossier");
        dialog.setHeaderText("Renommer le dossier");
        dialog.setContentText("Nouveau nom :");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty() && !newName.equals(folder.getName())) {
                System.out.println("Dossier renommé : " + folder.getName() + " → " + newName);
                refreshCallback.run();
            }
        });
    }

    public void handleDeleteFolder() {
        TreeItem<NodeItem> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;

        NodeItem folder = selected.getValue();
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le dossier");
        confirm.setHeaderText("Confirmer la suppression");
        
        boolean hasChildren = !folder.getChildren().isEmpty();
        boolean hasFiles = !folder.getFiles().isEmpty();
        
        String contentWarning = "";
        if (hasChildren || hasFiles) {
            int childCount = folder.getChildren().size();
            int fileCount = folder.getFiles().size();
            contentWarning = "\n\nCe dossier contient ";
            if (childCount > 0 && fileCount > 0) {
                contentWarning += childCount + " sous-dossier(s) et " + fileCount + " fichier(s).";
            } else if (childCount > 0) {
                contentWarning += childCount + " sous-dossier(s).";
            } else {
                contentWarning += fileCount + " fichier(s).";
            }
            contentWarning += "\n\nTous les éléments seront supprimés définitivement.";
        }
        
        confirm.setContentText("Voulez-vous vraiment supprimer le dossier '" + folder.getName() + "' ?" + contentWarning);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteFolderAsync(folder);
        }
    }

    private void deleteFolderAsync(NodeItem folder) {
        if (folder.getId() == null) {
            System.err.println("Erreur : ID du dossier introuvable");
            return;
        }

        javafx.concurrent.Task<Void> deleteTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                apiClient.deleteFolder(folder.getId());
                return null;
            }
        };

        deleteTask.setOnSucceeded(event -> {
            System.out.println("Dossier supprimé : " + folder.getName());
            refreshCallback.run();
        });

        deleteTask.setOnFailed(event -> {
            Throwable exception = deleteTask.getException();
            String errorMessage = "Erreur lors de la suppression du dossier";
            
            if (exception != null) {
                String exMsg = exception.getMessage();
                if (exMsg != null) {
                    if (exMsg.contains("404")) {
                        errorMessage = "Dossier introuvable.";
                    } else if (exMsg.contains("401")) {
                        errorMessage = "Session expirée. Veuillez vous reconnecter.";
                    } else if (exMsg.contains("403")) {
                        errorMessage = "Vous n'avez pas la permission de supprimer ce dossier.";
                    } else if (exMsg.contains("ConnectException")) {
                        errorMessage = "Impossible de contacter le serveur.";
                    } else {
                        errorMessage = "Erreur: " + exMsg;
                    }
                }
            }

            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Impossible de supprimer le dossier");
            error.setContentText(errorMessage);
            error.showAndWait();
        });

        Thread thread = new Thread(deleteTask, "delete-folder-task-thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void handleFolderProperties() {
        TreeItem<NodeItem> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;

        NodeItem folder = selected.getValue();
        
        int totalFolders = countTotalFolders(folder);
        int totalFiles = countTotalFiles(folder);
        long totalSize = calculateTotalSize(folder);
        
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Propriétés du dossier");
        info.setHeaderText("Propriétés de : " + folder.getName());
        info.setContentText(
            "Type : Dossier\n" +
            "Contenu :\n" +
            "  • " + folder.getChildren().size() + " sous-dossier(s) direct(s)\n" +
            "  • " + folder.getFiles().size() + " fichier(s) direct(s)\n" +
            "\nTotal (incluant les sous-dossiers) :\n" +
            "  • " + totalFolders + " dossier(s)\n" +
            "  • " + totalFiles + " fichier(s)\n" +
            "  • Taille totale : " + humanSize(totalSize)
        );
        info.showAndWait();
    }

    private int countTotalFolders(NodeItem folder) {
        int count = folder.getChildren().size();
        for (NodeItem child : folder.getChildren()) {
            count += countTotalFolders(child);
        }
        return count;
    }

    private int countTotalFiles(NodeItem folder) {
        int count = folder.getFiles().size();
        for (NodeItem child : folder.getChildren()) {
            count += countTotalFiles(child);
        }
        return count;
    }

    private long calculateTotalSize(NodeItem folder) {
        long size = folder.getFiles().stream().mapToLong(f -> f.getSize()).sum();
        for (NodeItem child : folder.getChildren()) {
            size += calculateTotalSize(child);
        }
        return size;
    }

    private String humanSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#")
            .format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
