package com.coffrefort.client.controllers;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.coffrefort.client.model.FileEntry;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class VersionController {

    public static class FileVersion {
        private int version;
        private long size;
        private Instant createdAt;
        private String checksum;
        private boolean current;

        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

        public String getChecksum() { return checksum; }
        public void setChecksum(String checksum) { this.checksum = checksum; }

        public boolean isCurrent() { return current; }
        public void setCurrent(boolean current) { this.current = current; }
    }

    private final Runnable refreshCallback;

    public VersionController(Runnable refreshCallback) {
        this.refreshCallback = refreshCallback;
    }

    public void handleVersionHistory(FileEntry selected, TableView<FileEntry> table) {
        if (selected == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Historique des versions");
        dialog.setHeaderText("Versions de : " + selected.getName());
        dialog.setResizable(true);
        
        TableView<FileVersion> versionsTable = new TableView<>();
        versionsTable.setPrefHeight(300);
        versionsTable.setPrefWidth(600);
        
        TableColumn<FileVersion, Integer> versionCol = new TableColumn<>("Version");
        versionCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("version"));
        versionCol.setPrefWidth(80);
        
        TableColumn<FileVersion, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getCreatedAt().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        ));
        dateCol.setPrefWidth(150);
        
        TableColumn<FileVersion, Long> sizeCol = new TableColumn<>("Taille");
        sizeCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("size"));
        sizeCol.setCellFactory(col -> new TableCell<>() {
            @Override 
            protected void updateItem(Long size, boolean empty) {
                super.updateItem(size, empty);
                setText(empty || size == null ? null : humanSize(size));
            }
        });
        sizeCol.setPrefWidth(100);
        
        TableColumn<FileVersion, String> checksumCol = new TableColumn<>("Checksum");
        checksumCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("checksum"));
        checksumCol.setPrefWidth(150);
        
        TableColumn<FileVersion, Boolean> currentCol = new TableColumn<>("Actuelle");
        currentCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("current"));
        currentCol.setCellFactory(col -> new TableCell<>() {
            @Override 
            protected void updateItem(Boolean current, boolean empty) {
                super.updateItem(current, empty);
                if (empty || current == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(current ? "✓" : "");
                    setStyle(current ? "-fx-background-color: #e8f5e9; -fx-font-weight: bold;" : "");
                }
            }
        });
        currentCol.setPrefWidth(80);
        
        versionsTable.getColumns().addAll(versionCol, dateCol, sizeCol, checksumCol, currentCol);
        
        List<FileVersion> versions = loadFileVersions(selected);
        versionsTable.getItems().setAll(versions);
        
        versions.stream()
            .filter(FileVersion::isCurrent)
            .findFirst()
            .ifPresent(v -> versionsTable.getSelectionModel().select(v));
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        Label infoLabel = new Label("Double-cliquez sur une version pour plus d'options");
        infoLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
        
        ToolBar toolbar = new ToolBar();
        
        Button downloadBtn = new Button("Télécharger");
        downloadBtn.setOnAction(e -> {
            FileVersion selectedVersion = versionsTable.getSelectionModel().getSelectedItem();
            if (selectedVersion != null) {
                downloadVersion(selected, selectedVersion, table);
            }
        });
        
        Button restoreBtn = new Button("Restaurer");
        restoreBtn.setOnAction(e -> {
            FileVersion selectedVersion = versionsTable.getSelectionModel().getSelectedItem();
            if (selectedVersion != null && !selectedVersion.isCurrent()) {
                restoreVersion(selected, selectedVersion);
                dialog.close();
            }
        });
        
        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setOnAction(e -> {
            FileVersion selectedVersion = versionsTable.getSelectionModel().getSelectedItem();
            if (selectedVersion != null && !selectedVersion.isCurrent()) {
                deleteVersion(selected, selectedVersion);
                versions.remove(selectedVersion);
                versionsTable.refresh();
            }
        });
        
        Button compareBtn = new Button("Comparer");
        compareBtn.setOnAction(e -> {
            FileVersion selectedVersion = versionsTable.getSelectionModel().getSelectedItem();
            if (selectedVersion != null) {
                compareVersions(selected, selectedVersion);
            }
        });
        
        toolbar.getItems().addAll(downloadBtn, restoreBtn, deleteBtn, new Separator(), compareBtn);
        
        versionsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSelection = newVal != null;
            boolean isCurrent = hasSelection && newVal.isCurrent();
            
            downloadBtn.setDisable(!hasSelection);
            restoreBtn.setDisable(!hasSelection || isCurrent);
            deleteBtn.setDisable(!hasSelection || isCurrent);
            compareBtn.setDisable(!hasSelection || isCurrent);
        });
        
        Label statsLabel = new Label();
        updateVersionStats(statsLabel, versions);
        statsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        content.getChildren().addAll(infoLabel, versionsTable, toolbar, statsLabel);
        VBox.setVgrow(versionsTable, Priority.ALWAYS);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        
        versionsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                FileVersion selectedVersion = versionsTable.getSelectionModel().getSelectedItem();
                if (selectedVersion != null) {
                    showVersionDetails(selected, selectedVersion);
                }
            }
        });
        
        dialog.showAndWait();
    }

    private List<FileVersion> loadFileVersions(FileEntry file) {
        List<FileVersion> versions = new ArrayList<>();
        
        for (int i = 5; i >= 1; i--) {
            FileVersion version = new FileVersion();
            version.setVersion(i);
            version.setSize(file.getSize() + (i * 1024));
            version.setCreatedAt(file.getUpdatedAt().minusSeconds(86400L * (6 - i)));
            version.setChecksum("sha256:" + Integer.toHexString((file.getName() + i).hashCode()));
            version.setCurrent(i == 5);
            versions.add(version);
        }
        
        return versions;
    }

    private void downloadVersion(FileEntry file, FileVersion version, TableView<FileEntry> table) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer la version " + version.getVersion());
        chooser.setInitialFileName(file.getName() + ".v" + version.getVersion());
        java.io.File dest = chooser.showSaveDialog(table.getScene().getWindow());
        
        if (dest != null) {
            System.out.println("Téléchargement de la version " + version.getVersion() + "...");
        }
    }

    private void restoreVersion(FileEntry file, FileVersion version) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restaurer une version");
        confirm.setHeaderText("Confirmer la restauration");
        confirm.setContentText(
            "Voulez-vous restaurer la version " + version.getVersion() + " ?\n\n" +
            "Cela créera une nouvelle version actuelle avec le contenu de la version " + 
            version.getVersion() + "."
        );
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("Version " + version.getVersion() + " restaurée");
            refreshCallback.run();
        }
    }

    private void deleteVersion(FileEntry file, FileVersion version) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer une version");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText(
            "Voulez-vous vraiment supprimer la version " + version.getVersion() + " ?\n\n" +
            "Cette action est irréversible."
        );
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("Version " + version.getVersion() + " supprimée");
        }
    }

    private void compareVersions(FileEntry file, FileVersion version) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Comparaison de versions");
        info.setHeaderText("Comparaison : version actuelle vs version " + version.getVersion());
        info.setContentText(
            "Différences de métadonnées :\n\n" +
            "Taille actuelle : " + humanSize(file.getSize()) + "\n" +
            "Taille v" + version.getVersion() + " : " + humanSize(version.getSize()) + "\n" +
            "Différence : " + humanSize(Math.abs(file.getSize() - version.getSize())) + "\n\n" +
            "Pour une comparaison détaillée du contenu, téléchargez les deux versions."
        );
        
        info.showAndWait();
    }

    private void showVersionDetails(FileEntry file, FileVersion version) {
        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Détails de la version");
        details.setHeaderText("Version " + version.getVersion() + " de " + file.getName());
        
        String content = "Taille : " + humanSize(version.getSize()) + " (" + version.getSize() + " octets)\n" +
                        "Créée le : " + version.getCreatedAt().atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n" +
                        "Checksum : " + version.getChecksum() + "\n" +
                        "Statut : " + (version.isCurrent() ? "Version actuelle" : "Version archivée");
        
        details.setContentText(content);
        details.showAndWait();
    }

    private void updateVersionStats(Label label, List<FileVersion> versions) {
        long totalSize = versions.stream().mapToLong(FileVersion::getSize).sum();
        int count = versions.size();
        
        label.setText(String.format(
            "Total : %d version(s) • Espace utilisé : %s • Moyenne : %s par version",
            count,
            humanSize(totalSize),
            humanSize(count > 0 ? totalSize / count : 0)
        ));
    }

    private String humanSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#")
            .format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
