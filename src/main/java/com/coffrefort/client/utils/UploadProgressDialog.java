package com.coffrefort.client.utils;

import java.io.File;
import java.text.DecimalFormat;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Dialogue personnalisé pour afficher la progression d'un upload
 */
public class UploadProgressDialog extends Dialog<ButtonType> {
    
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Label speedLabel;
    private final Button cancelButton;
    
    private long startTime;
    private long totalBytes;
    private long uploadedBytes;
    
    public UploadProgressDialog(File file) {
        setTitle("Upload en cours");
        setHeaderText("Upload de : " + file.getName());
        
        // Rendre le dialogue modal
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        
        // Empêcher la fermeture avec X
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.setOnCloseRequest(event -> event.consume());
        
        // Contenu
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);
        
        // Barre de progression
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(380);
        
        // Labels d'information
        statusLabel = new Label("Préparation de l'upload...");
        speedLabel = new Label("");
        
        Label sizeLabel = new Label("Taille : " + humanSize(file.length()));
        sizeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        
        content.getChildren().addAll(
            progressBar,
            statusLabel,
            speedLabel,
            sizeLabel
        );
        
        getDialogPane().setContent(content);
        
        // Bouton annuler
        cancelButton = new Button("Annuler");
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        
        this.totalBytes = file.length();
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * Met à jour la progression
     * @param uploadedBytes Nombre d'octets uploadés
     */
    public void updateProgress(long uploadedBytes) {
        this.uploadedBytes = uploadedBytes;
        
        double progress = totalBytes > 0 ? (double) uploadedBytes / totalBytes : 0;
        progressBar.setProgress(progress);
        
        int percentage = (int) (progress * 100);
        statusLabel.setText(percentage + "% - " + humanSize(uploadedBytes) + " / " + humanSize(totalBytes));
        
        // Calculer la vitesse
        long elapsedMs = System.currentTimeMillis() - startTime;
        if (elapsedMs > 0) {
            double speedBytesPerSec = (double) uploadedBytes / (elapsedMs / 1000.0);
            speedLabel.setText("Vitesse : " + humanSize((long) speedBytesPerSec) + "/s");
            
            // Estimation du temps restant
            if (uploadedBytes > 0 && uploadedBytes < totalBytes) {
                long remainingBytes = totalBytes - uploadedBytes;
                long remainingSec = (long) (remainingBytes / speedBytesPerSec);
                speedLabel.setText(speedLabel.getText() + " - Temps restant : " + formatDuration(remainingSec));
            }
        }
    }
    
    /**
     * Marque l'upload comme terminé
     */
    public void setCompleted() {
        progressBar.setProgress(1.0);
        statusLabel.setText("Upload terminé !");
        speedLabel.setText("");
        
        getDialogPane().getButtonTypes().clear();
        getDialogPane().getButtonTypes().add(ButtonType.OK);
    }
    
    /**
     * Marque l'upload comme échoué
     */
    public void setFailed(String error) {
        statusLabel.setText("Échec de l'upload");
        statusLabel.setStyle("-fx-text-fill: #f44336;");
        speedLabel.setText(error);
        speedLabel.setStyle("-fx-text-fill: #f44336;");
        
        getDialogPane().getButtonTypes().clear();
        getDialogPane().getButtonTypes().add(ButtonType.OK);
    }
    
    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        double v = bytes;
        String[] units = {"Ko", "Mo", "Go", "To"};
        int i = -1;
        while (v >= 1024 && i < units.length - 1) { 
            v /= 1024.0; 
            i++; 
        }
        return new DecimalFormat("0.##").format(v) + " " + units[i];
    }
    
    private static String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long secs = seconds % 60;
            return minutes + "m " + secs + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
}