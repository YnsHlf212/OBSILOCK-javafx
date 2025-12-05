package com.coffrefort.client.controllers;

import java.io.File;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.NodeItem;

import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.FileChooser;

public class UploadController {
    private final ApiClient apiClient;
    private final javafx.scene.control.TableView<?> table;
    private final Runnable refreshCallback;

    public UploadController(ApiClient apiClient, javafx.scene.control.TableView<?> table, 
                           Runnable refreshCallback) {
        this.apiClient = apiClient;
        this.table = table;
        this.refreshCallback = refreshCallback;
    }

    public void handleUpload(NodeItem currentFolder) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier à envoyer");
        File file = chooser.showOpenDialog(table.getScene().getWindow());
        if (file != null) {
            uploadFileAsync(file, currentFolder);
        }
    }

    private void uploadFileAsync(File file, NodeItem currentFolder) {
        System.out.println("\n=== uploadFileAsync() appelée ===");
        System.out.println("Fichier: " + file.getName());
        System.out.println("Taille: " + file.length());
        System.out.println("Parent ID: " + (currentFolder != null ? currentFolder.getId() : "null"));
        
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Upload en cours");
        progressAlert.setHeaderText("Upload de : " + file.getName());
        progressAlert.setContentText("Veuillez patienter...");
        
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressAlert.setGraphic(progressIndicator);
        
        progressAlert.getButtonTypes().clear();
        progressAlert.show();
        System.out.println("Dialog de progression affiché");
        
        Integer folderId = currentFolder != null ? currentFolder.getId() : null;
        
        javafx.concurrent.Task<Integer> uploadTask = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() throws Exception {
                System.out.println("\n=== Upload Task.call() exécutée ===");
                System.out.println("Thread: " + Thread.currentThread().getName());
                System.out.println("Appel apiClient.uploadFile()");
                
                try {
                    Integer result = apiClient.uploadFile(file, folderId);
                    System.out.println("Résultat: " + result);
                    return result;
                } catch (Exception e) {
                    System.err.println("Exception dans Upload Task.call(): " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }
        };
        
        uploadTask.setOnSucceeded(event -> {
            System.out.println("\n=== Upload Task SUCCEEDED ===");
            progressAlert.close();
            
            Integer fileId = uploadTask.getValue();
            System.out.println("ID du fichier: " + fileId);
            
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Upload réussi");
            success.setHeaderText("Fichier uploadé avec succès");
            success.setContentText("Le fichier '" + file.getName() + "' a été uploadé.\n" +
                                  (fileId != null ? "ID: " + fileId : ""));
            success.showAndWait();
            
            System.out.println("Rafraîchissement de l'affichage...");
            refreshCallback.run();
        });
        
        uploadTask.setOnFailed(event -> {
            System.err.println("\n=== Upload Task FAILED ===");
            progressAlert.close();
            
            Throwable exception = uploadTask.getException();
            System.err.println("Exception: " + (exception != null ? exception.getClass().getName() : "null"));
            System.err.println("Message: " + (exception != null ? exception.getMessage() : "null"));
            
            String errorMessage = "Erreur lors de l'upload";
            
            if (exception != null) {
                exception.printStackTrace();
                String exMsg = exception.getMessage();
                
                if (exMsg != null) {
                    if (exMsg.contains("413")) {
                        errorMessage = "Fichier trop volumineux.\nLa taille maximale autorisée est dépassée.";
                    } else if (exMsg.contains("415")) {
                        errorMessage = "Type de fichier non autorisé.";
                    } else if (exMsg.contains("507")) {
                        errorMessage = "Espace de stockage insuffisant.";
                    } else if (exMsg.contains("401")) {
                        errorMessage = "Session expirée. Veuillez vous reconnecter.";
                    } else if (exMsg.contains("ConnectException") || exMsg.contains("Connection refused")) {
                        errorMessage = "Impossible de contacter le serveur.\nVérifiez que le backend est démarré.";
                    } else if (exMsg.contains("timeout")) {
                        errorMessage = "Délai d'attente dépassé.\nLe fichier est peut-être trop volumineux.";
                    } else {
                        errorMessage = "Erreur: " + exMsg;
                    }
                }
            }
            
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur d'upload");
            error.setHeaderText("Impossible d'uploader le fichier");
            error.setContentText(errorMessage);
            error.showAndWait();
        });
        
        Thread thread = new Thread(uploadTask, "upload-task-thread");
        thread.setDaemon(true);
        System.out.println("Démarrage du thread upload...");
        thread.start();
        System.out.println("Thread upload démarré");
    }
}
