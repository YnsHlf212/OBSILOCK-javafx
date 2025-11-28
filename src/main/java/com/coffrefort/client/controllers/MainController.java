package com.coffrefort.client.controllers;

import java.io.File;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;
import com.coffrefort.client.model.Quota;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class MainController {
    @FXML private TreeView<NodeItem> treeView;
    @FXML private TableView<FileEntry> table;
    @FXML private TableColumn<FileEntry, String> nameCol;
    @FXML private TableColumn<FileEntry, Long> sizeCol;
    @FXML private TableColumn<FileEntry, String> dateCol;
    @FXML private TableColumn<FileEntry, Integer> versionCol;
    @FXML private ProgressBar quotaBar;
    @FXML private Label quotaLabel;
    @FXML private Button createFolderBtn;
    @FXML private Label userEmailLabel;
    @FXML private Button logoutBtn;

    private ApiClient apiClient;
    
    // Clipboard interne pour gérer copier/couper/coller
    private FileEntry clipboardFile;
    private boolean isCutOperation; // true = couper, false = copier
    private NodeItem currentFolder;
    private String userEmail; // Email de l'utilisateur connecté

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
        if (treeView != null) {
            loadData();
        }
    }

    public void setUserEmail(String email) {
        this.userEmail = email;
        if (userEmailLabel != null) {
            userEmailLabel.setText(email);
        }
    }

    @FXML
    private void initialize() {
        setupTableColumns();
        setupTreeViewListener();
        setupContextMenu();
        setupDragAndDrop();
        setupTreeContextMenu();
        
        // Initialiser l'email utilisateur si déjà défini
        if (userEmail != null && userEmailLabel != null) {
            userEmailLabel.setText(userEmail);
        }
        
        if (apiClient != null) {
            loadData();
        }
    }

    private void setupTableColumns() {
        if (nameCol != null) {
            nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        }
        if (sizeCol != null) {
            sizeCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("size"));
            sizeCol.setCellFactory(col -> new TableCell<>() {
                @Override 
                protected void updateItem(Long size, boolean empty) {
                    super.updateItem(size, empty);
                    setText(empty || size == null ? null : humanSize(size));
                }
            });
        }
        if (dateCol != null) {
            dateCol.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getUpdatedAt().atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            ));
        }
        if (versionCol != null) {
            versionCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("currentVersion"));
            versionCol.setCellFactory(col -> new TableCell<>() {
                @Override 
                protected void updateItem(Integer version, boolean empty) {
                    super.updateItem(version, empty);
                    if (empty || version == null || version == 0) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText("v" + version);
                        setStyle("-fx-alignment: CENTER; -fx-text-fill: #0078d4; -fx-font-weight: bold;");
                    }
                }
            });
        }
    }

    private void setupTreeViewListener() {
        if (treeView != null) {
            treeView.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
                if (sel != null) {
                    currentFolder = sel.getValue();
                    refreshFiles(currentFolder);
                }
            });
        }
    }

    /**
     * Configure le menu contextuel (clic droit) sur la TableView
     */
    private void setupContextMenu() {
        if (table == null) return;

        ContextMenu contextMenu = new ContextMenu();

        // Menu Copier
        MenuItem copyItem = new MenuItem("Copier");
        copyItem.setOnAction(e -> handleCopy());

        // Menu Couper
        MenuItem cutItem = new MenuItem("Couper");
        cutItem.setOnAction(e -> handleCut());

        // Menu Coller
        MenuItem pasteItem = new MenuItem("Coller");
        pasteItem.setOnAction(e -> handlePaste());

        // Séparateur
        SeparatorMenuItem separator1 = new SeparatorMenuItem();

        // Menu Renommer
        MenuItem renameItem = new MenuItem("Renommer");
        renameItem.setOnAction(e -> handleRename());

        // Menu Supprimer
        MenuItem deleteItem = new MenuItem("Supprimer");
        deleteItem.setOnAction(e -> handleDelete());

        // Séparateur
        SeparatorMenuItem separator2 = new SeparatorMenuItem();

        // Menu Télécharger
        MenuItem downloadItem = new MenuItem("Télécharger");
        downloadItem.setOnAction(e -> handleDownload());

        // Menu Partager
        MenuItem shareItem = new MenuItem("Créer un lien de partage");
        shareItem.setOnAction(e -> handleShare());

        // Séparateur
        SeparatorMenuItem separator3 = new SeparatorMenuItem();

        // Menu Propriétés
        MenuItem propertiesItem = new MenuItem("Propriétés");
        propertiesItem.setOnAction(e -> handleProperties());

        // Menu Historique des versions
        MenuItem versionsItem = new MenuItem("Historique des versions");
        versionsItem.setOnAction(e -> handleVersionHistory());

        contextMenu.getItems().addAll(
                copyItem, cutItem, pasteItem,
                separator1,
                renameItem, deleteItem,
                separator2,
                downloadItem, shareItem,
                separator3,
                propertiesItem, versionsItem
        );

        // Activer/désactiver dynamiquement les options selon le contexte
        contextMenu.setOnShowing(e -> {
            FileEntry selected = table.getSelectionModel().getSelectedItem();
            boolean hasSelection = selected != null;
            boolean hasClipboard = clipboardFile != null;

            copyItem.setDisable(!hasSelection);
            cutItem.setDisable(!hasSelection);
            pasteItem.setDisable(!hasClipboard);
            renameItem.setDisable(!hasSelection);
            deleteItem.setDisable(!hasSelection);
            downloadItem.setDisable(!hasSelection);
            shareItem.setDisable(!hasSelection);
            propertiesItem.setDisable(!hasSelection);
            versionsItem.setDisable(!hasSelection);
        });

        table.setContextMenu(contextMenu);
    }

    /**
     * Configure le menu contextuel (clic droit) sur le TreeView (dossiers)
     */
    private void setupTreeContextMenu() {
        if (treeView == null) return;

        ContextMenu treeContextMenu = new ContextMenu();

        // Menu Nouveau dossier
        MenuItem newFolderItem = new MenuItem("Nouveau dossier");
        newFolderItem.setOnAction(e -> handleCreateFolder());

        // Menu Renommer le dossier
        MenuItem renameFolderItem = new MenuItem("Renommer");
        renameFolderItem.setOnAction(e -> handleRenameFolder());

        // Menu Supprimer le dossier
        MenuItem deleteFolderItem = new MenuItem("Supprimer");
        deleteFolderItem.setOnAction(e -> handleDeleteFolder());

        // Séparateur
        SeparatorMenuItem separator = new SeparatorMenuItem();

        // Menu Propriétés du dossier
        MenuItem folderPropsItem = new MenuItem("Propriétés");
        folderPropsItem.setOnAction(e -> handleFolderProperties());

        // Menu Actualiser
        MenuItem refreshItem = new MenuItem("Actualiser");
        refreshItem.setOnAction(e -> loadData());

        treeContextMenu.getItems().addAll(
                newFolderItem,
                separator,
                renameFolderItem,
                deleteFolderItem,
                separator,
                folderPropsItem,
                refreshItem
        );

        // Activer/désactiver dynamiquement selon la sélection
        treeContextMenu.setOnShowing(e -> {
            TreeItem<NodeItem> selected = treeView.getSelectionModel().getSelectedItem();
            boolean hasSelection = selected != null && selected.getValue() != null;

            renameFolderItem.setDisable(!hasSelection);
            deleteFolderItem.setDisable(!hasSelection);
            folderPropsItem.setDisable(!hasSelection);
        });

        treeView.setContextMenu(treeContextMenu);
    }

    /**
     * Configure le Drag & Drop pour déplacer des fichiers
     */
    private void setupDragAndDrop() {
        setupTableDragSource();
        setupTreeDragTarget();
        setupTableDragTarget();
    }

    /**
     * Configure la TableView comme source de drag (fichiers)
     */
    private void setupTableDragSource() {
        if (table == null) return;

        table.setOnDragDetected(event -> {
            FileEntry selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                javafx.scene.input.Dragboard db = table.startDragAndDrop(
                    javafx.scene.input.TransferMode.MOVE
                );
                
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(selected.getName());
                db.setContent(content);
                
                // Stocker le fichier en cours de déplacement
                clipboardFile = selected;
                isCutOperation = true;
                
                event.consume();
            }
        });

        table.setOnDragDone(event -> {
            if (event.getTransferMode() == javafx.scene.input.TransferMode.MOVE) {
                // Le déplacement est géré dans le drop target
            }
            event.consume();
        });
    }

    /**
     * Configure le TreeView comme cible de drop (dossiers)
     */
    private void setupTreeDragTarget() {
        if (treeView == null) return;

        treeView.setOnDragOver(event -> {
            if (event.getGestureSource() != treeView && 
                event.getDragboard().hasString()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
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
            javafx.scene.input.Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasString() && clipboardFile != null) {
                // Récupérer le dossier cible
                TreeItem<NodeItem> targetItem = treeView.getSelectionModel().getSelectedItem();
                if (targetItem != null) {
                    NodeItem targetFolder = targetItem.getValue();
                    
                    // Confirmation du déplacement
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Déplacer le fichier");
                    confirm.setHeaderText("Confirmer le déplacement");
                    confirm.setContentText("Déplacer '" + clipboardFile.getName() + 
                                          "' vers '" + targetFolder.getName() + "' ?");
                    
                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        // TODO: Appeler l'API pour déplacer le fichier
                        showStatus("Fichier déplacé vers : " + targetFolder.getName());
                        
                        // Rafraîchir l'affichage
                        refreshFiles(currentFolder);
                        success = true;
                    }
                }
                
                clipboardFile = null;
            }
            
            event.setDropCompleted(success);
            treeView.setStyle("");
            event.consume();
        });
    }

    /**
     * Configure la TableView comme cible de drop (réorganisation)
     */
    private void setupTableDragTarget() {
        if (table == null) return;

        table.setOnDragOver(event -> {
            if (event.getGestureSource() == table && 
                event.getDragboard().hasString()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
            }
            event.consume();
        });

        table.setOnDragDropped(event -> {
            javafx.scene.input.Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasString() && clipboardFile != null) {
                // Déplacement dans le même dossier (réorganisation)
                showStatus("Réorganisation dans le dossier actuel");
                success = true;
                clipboardFile = null;
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Gère l'action Copier
     */
    private void handleCopy() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            clipboardFile = selected;
            isCutOperation = false;
            showStatus("Fichier copié : " + selected.getName());
        }
    }

    /**
     * Gère l'action Couper
     */
    private void handleCut() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            clipboardFile = selected;
            isCutOperation = true;
            showStatus("Fichier coupé : " + selected.getName());
        }
    }

    /**
     * Gère l'action Coller
     */
    private void handlePaste() {
        if (clipboardFile == null || currentFolder == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Coller");
        confirm.setHeaderText(isCutOperation ? "Déplacer le fichier" : "Copier le fichier");
        confirm.setContentText("Voulez-vous " + (isCutOperation ? "déplacer" : "copier") + 
                              " '" + clipboardFile.getName() + "' dans '" + currentFolder.getName() + "' ?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: Appeler l'API pour copier/déplacer le fichier
            if (isCutOperation) {
                showStatus("Fichier déplacé : " + clipboardFile.getName());
                clipboardFile = null; // Nettoyer après déplacement
            } else {
                showStatus("Fichier copié : " + clipboardFile.getName());
            }
            refreshFiles(currentFolder);
        }
    }

    /**
     * Gère l'action Renommer
     */
    private void handleRename() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Renommer");
        dialog.setHeaderText("Renommer le fichier");
        dialog.setContentText("Nouveau nom :");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                // TODO: Appeler l'API pour renommer le fichier
                showStatus("Fichier renommé : " + selected.getName() + " → " + newName);
                refreshFiles(currentFolder);
            }
        });
    }

    /**
     * Gère l'action Supprimer
     */
    private void handleDelete() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Voulez-vous vraiment supprimer '" + selected.getName() + "' ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: Appeler l'API pour supprimer le fichier
            showStatus("Fichier supprimé : " + selected.getName());
            refreshFiles(currentFolder);
        }
    }

    /**
     * Gère l'action Télécharger
     */
    private void handleDownload() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le fichier");
        chooser.setInitialFileName(selected.getName());
        File file = chooser.showSaveDialog(table.getScene().getWindow());
        
        if (file != null) {
            // TODO: Appeler l'API pour télécharger le fichier
            showStatus("Téléchargement de : " + selected.getName());
        }
    }

    /**
     * Gère l'action Partager (créer un lien)
     */
    private void handleShare() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Créer un lien de partage");
        dialog.setHeaderText("Partager : " + selected.getName());

        // Contenu du dialogue
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField labelField = new TextField("Mon partage");
        TextField expiryField = new TextField("7");
        TextField maxUsesField = new TextField("10");

        grid.add(new Label("Nom du lien :"), 0, 0);
        grid.add(labelField, 1, 0);
        grid.add(new Label("Expiration (jours) :"), 0, 1);
        grid.add(expiryField, 1, 1);
        grid.add(new Label("Usages max :"), 0, 2);
        grid.add(maxUsesField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: Appeler l'API pour créer le lien de partage
            String shareUrl = "https://coffre.example.com/s/abc123xyz";
            
            // Copier dans le presse-papiers système
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

    /**
     * Gère l'action Propriétés
     */
    private void handleProperties() {
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

    /**
     * Gère l'affichage de l'historique des versions
     */
    private void handleVersionHistory() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Créer un dialogue personnalisé pour l'historique
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Historique des versions");
        dialog.setHeaderText("Versions de : " + selected.getName());
        dialog.setResizable(true);
        
        // Créer la TableView pour les versions
        TableView<FileVersion> versionsTable = new TableView<>();
        versionsTable.setPrefHeight(300);
        versionsTable.setPrefWidth(600);
        
        // Colonnes
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
        
        // Charger les versions (simulées pour l'instant)
        List<FileVersion> versions = loadFileVersions(selected);
        versionsTable.getItems().setAll(versions);
        
        // Sélectionner la version actuelle par défaut
        versions.stream()
            .filter(FileVersion::isCurrent)
            .findFirst()
            .ifPresent(v -> versionsTable.getSelectionModel().select(v));
        
        // Boutons d'action
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        Label infoLabel = new Label("Double-cliquez sur une version pour plus d'options");
        infoLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
        
        // Barre d'outils
        ToolBar toolbar = new ToolBar();
        
        Button downloadBtn = new Button("Télécharger");
        downloadBtn.setOnAction(e -> {
            FileVersion selectedVersion = versionsTable.getSelectionModel().getSelectedItem();
            if (selectedVersion != null) {
                downloadVersion(selected, selectedVersion);
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
        
        // Désactiver les boutons si aucune version sélectionnée
        versionsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSelection = newVal != null;
            boolean isCurrent = hasSelection && newVal.isCurrent();
            
            downloadBtn.setDisable(!hasSelection);
            restoreBtn.setDisable(!hasSelection || isCurrent);
            deleteBtn.setDisable(!hasSelection || isCurrent);
            compareBtn.setDisable(!hasSelection || isCurrent);
        });
        
        // Informations supplémentaires
        Label statsLabel = new Label();
        updateVersionStats(statsLabel, versions);
        statsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        content.getChildren().addAll(infoLabel, versionsTable, toolbar, statsLabel);
        VBox.setVgrow(versionsTable, Priority.ALWAYS);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        
        // Double-clic pour voir les détails
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

    /**
     * Charge les versions d'un fichier (simulé pour l'instant)
     */
    private List<FileVersion> loadFileVersions(FileEntry file) {
        // TODO: Appeler l'API pour charger les vraies versions
        List<FileVersion> versions = new ArrayList<>();
        
        // Simuler 5 versions
        for (int i = 5; i >= 1; i--) {
            FileVersion version = new FileVersion();
            version.setVersion(i);
            version.setSize(file.getSize() + (i * 1024));
            version.setCreatedAt(file.getUpdatedAt().minusSeconds(86400L * (6 - i)));
            version.setChecksum("sha256:" + Integer.toHexString((file.getName() + i).hashCode()));
            version.setCurrent(i == 5); // La version 5 est la version actuelle
            versions.add(version);
        }
        
        return versions;
    }

    /**
     * Télécharge une version spécifique
     */
    private void downloadVersion(FileEntry file, FileVersion version) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer la version " + version.getVersion());
        chooser.setInitialFileName(file.getName() + ".v" + version.getVersion());
        File dest = chooser.showSaveDialog(table.getScene().getWindow());
        
        if (dest != null) {
            // TODO: Appeler l'API pour télécharger cette version
            showStatus("Téléchargement de la version " + version.getVersion() + "...");
        }
    }

    /**
     * Restaure une version ancienne comme version actuelle
     */
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
            // TODO: Appeler l'API pour restaurer cette version
            showStatus("Version " + version.getVersion() + " restaurée");
            refreshFiles(currentFolder);
        }
    }

    /**
     * Supprime une version ancienne
     */
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
            // TODO: Appeler l'API pour supprimer cette version
            showStatus("Version " + version.getVersion() + " supprimée");
        }
    }

    /**
     * Compare deux versions
     */
    private void compareVersions(FileEntry file, FileVersion version) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Comparaison de versions");
        info.setHeaderText("Comparaison : version actuelle vs version " + version.getVersion());
        
        // TODO: Implémenter une vraie comparaison (diff de contenu)
        info.setContentText(
            "Différences de métadonnées :\n\n" +
            "Taille actuelle : " + humanSize(file.getSize()) + "\n" +
            "Taille v" + version.getVersion() + " : " + humanSize(version.getSize()) + "\n" +
            "Différence : " + humanSize(Math.abs(file.getSize() - version.getSize())) + "\n\n" +
            "Pour une comparaison détaillée du contenu, téléchargez les deux versions."
        );
        
        info.showAndWait();
    }

    /**
     * Affiche les détails d'une version
     */
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

    /**
     * Met à jour les statistiques des versions
     */
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

    /**
     * Classe interne pour représenter une version de fichier
     */
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

    /**
     * Gère la déconnexion de l'utilisateur
     */
    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText("Confirmer la déconnexion");
        confirm.setContentText("Voulez-vous vraiment vous déconnecter ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Nettoyer le token d'authentification
                apiClient.clearToken();
                System.out.println("Token nettoyé, déconnexion effectuée");
                
                // Retour à l'écran de connexion
                returnToLoginScreen();
            } catch (Exception e) {
                System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
                e.printStackTrace();
                
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Erreur");
                error.setHeaderText("Erreur lors de la déconnexion");
                error.setContentText(e.getMessage());
                error.showAndWait();
            }
        }
    }

    /**
     * Retourne à l'écran de connexion
     */
    private void returnToLoginScreen() {
        try {
            // Charger l'écran de connexion
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/coffrefort/client/login.fxml")
            );
            
            // IMPORTANT: Configurer le controller factory pour injecter l'ApiClient
            loader.setControllerFactory(type -> {
                if (type == LoginController.class) {
                    LoginController controller = new LoginController();
                    controller.setApiClient(apiClient); // Réutiliser l'ApiClient existant
                    controller.setOnSuccess((email) -> {
                        // Stocker l'email
                        userEmail = email;
                        // Ouvrir à nouveau la fenêtre principale
                        openMainWindow();
                    });
                    return controller;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            javafx.scene.Parent root = loader.load();
            
            // Créer une nouvelle fenêtre de connexion
            javafx.stage.Stage loginStage = new javafx.stage.Stage();
            loginStage.setTitle("Coffre-fort numérique — Connexion");
            loginStage.setScene(new javafx.scene.Scene(root, 420, 320));
            
            // Fermer la fenêtre actuelle
            javafx.stage.Stage currentStage = (javafx.stage.Stage) logoutBtn.getScene().getWindow();
            currentStage.close();
            
            // Afficher la fenêtre de connexion
            loginStage.show();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du retour à l'écran de connexion:");
            e.printStackTrace();
            
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Impossible de retourner à l'écran de connexion");
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }

    /**
     * Ouvre la fenêtre principale après reconnexion
     */
    private void openMainWindow() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/coffrefort/client/main.fxml")
            );
            
            loader.setControllerFactory(type -> {
                if (type == MainController.class) {
                    MainController controller = new MainController();
                    controller.setApiClient(apiClient);
                    controller.setUserEmail(userEmail);
                    return controller;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage mainStage = new javafx.stage.Stage();
            mainStage.setTitle("Coffre-fort — Espace personnel");
            mainStage.setScene(new javafx.scene.Scene(root, 1024, 680));
            
            // Fermer la fenêtre de login actuelle
            javafx.stage.Stage loginStage = (javafx.stage.Stage) 
                javafx.stage.Stage.getWindows().stream()
                    .filter(w -> w instanceof javafx.stage.Stage)
                    .findFirst()
                    .orElse(null);
            
            if (loginStage != null) {
                loginStage.close();
            }
            
            mainStage.show();
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture de la fenêtre principale:");
            e.printStackTrace();
            
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Impossible d'ouvrir la fenêtre principale");
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }



    /**
     * Affiche un message de statut (à améliorer avec un vrai bandeau)
     */
    private void showStatus(String message) {
        if (quotaLabel != null) {
            String currentText = quotaLabel.getText();
            quotaLabel.setText(message);
            
            // Restaurer le texte original après 3 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    javafx.application.Platform.runLater(() -> quotaLabel.setText(currentText));
                } catch (InterruptedException ignored) {}
            }).start();
        }
    }

    private void loadData() {
        List<NodeItem> roots = apiClient.listRoot();
        TreeItem<NodeItem> hiddenRoot = new TreeItem<>(NodeItem.folder("root"));
        for (NodeItem n : roots) {
            hiddenRoot.getChildren().add(buildTree(n));
        }
        treeView.setRoot(hiddenRoot);
        if (!hiddenRoot.getChildren().isEmpty()) {
            treeView.getSelectionModel().select(hiddenRoot.getChildren().get(0));
        }

        Quota q = apiClient.getQuota();
        quotaBar.setProgress(q.getUsageRatio());
        quotaLabel.setText(humanSize(q.getUsed()) + " / " + humanSize(q.getMax()));
    }

    private TreeItem<NodeItem> buildTree(NodeItem node) {
        TreeItem<NodeItem> ti = new TreeItem<>(node);
        for (NodeItem child : node.getChildren()) {
            ti.getChildren().add(buildTree(child));
        }
        ti.setExpanded(true);
        return ti;
    }

    private void refreshFiles(NodeItem node) {
        table.getItems().setAll(node.getFiles());
    }

    @FXML
    private void handleUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier à envoyer");
        File file = chooser.showOpenDialog(table.getScene().getWindow());
        if (file != null) {
            // TODO: Implémenter le vrai upload avec progression
            showStatus("Upload de : " + file.getName());
        }
    }

    /**
     * Gère la création d'un nouveau dossier (depuis le bouton)
     */
    @FXML
    private void handleCreateFolder() {
        createNewFolder();
    }

    /**
     * Crée un nouveau dossier dans le dossier actuellement sélectionné
     */
    private void createNewFolder() {
        System.out.println("\n=== createNewFolder() appelée ===");
        System.out.println("currentFolder: " + (currentFolder != null ? currentFolder.getName() : "null"));
        System.out.println("currentFolder ID: " + (currentFolder != null ? currentFolder.getId() : "null"));
        System.out.println("apiClient: " + (apiClient != null ? "initialized" : "null"));
        System.out.println("apiClient.isAuthenticated(): " + (apiClient != null ? apiClient.isAuthenticated() : "N/A"));
        
        // Dialogue pour saisir le nom du dossier
        TextInputDialog dialog = new TextInputDialog("Nouveau dossier");
        dialog.setTitle("Créer un dossier");
        dialog.setHeaderText("Créer un nouveau dossier" + 
            (currentFolder != null ? " dans '" + currentFolder.getName() + "'" : ""));
        dialog.setContentText("Nom du dossier :");

        // Personnaliser le dialogue
        dialog.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            // Validation en temps réel
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
                // Appeler l'API pour créer le dossier de manière asynchrone
                createFolderAsync(trimmedName);
            } else {
                System.out.println("Nom vide, annulation");
            }
        });
    }

    /**
     * Crée un dossier de manière asynchrone
     */
    private void createFolderAsync(String folderName) {
        System.out.println("\n=== createFolderAsync() appelée ===");
        System.out.println("Nom du dossier: " + folderName);
        System.out.println("Parent ID: " + (currentFolder != null ? currentFolder.getId() : "null"));
        
        // Créer un dialogue de progression
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Création en cours");
        progressAlert.setHeaderText("Création du dossier : " + folderName);
        progressAlert.setContentText("Veuillez patienter...");
        
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressAlert.setGraphic(progressIndicator);
        
        // Retirer le bouton OK pour empêcher la fermeture
        progressAlert.getButtonTypes().clear();
        
        // Afficher le dialogue sans bloquer
        progressAlert.show();
        System.out.println("Dialog de progression affiché");
        
        // Récupérer le parent_id du dossier sélectionné
        Integer parentId = currentFolder != null ? currentFolder.getId() : null;
        
        // Créer une Task pour la création
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
        
        // Gestion du succès
        createTask.setOnSucceeded(event -> {
            System.out.println("\n=== Task SUCCEEDED ===");
            
            // IMPORTANT: Utiliser Platform.runLater pour s'assurer que la fermeture se fait sur le thread JavaFX
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
                    
                    // Rafraîchir l'affichage
                    System.out.println("Rafraîchissement de l'affichage...");
                    loadData();
                    showStatus("Dossier créé : " + folderName);
                } catch (Exception e) {
                    System.err.println("Erreur dans Platform.runLater (success): " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
        
        // Gestion des erreurs
        createTask.setOnFailed(event -> {
            System.err.println("\n=== Task FAILED ===");
            
            // IMPORTANT: Utiliser Platform.runLater pour s'assurer que la fermeture se fait sur le thread JavaFX
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
                    
                    showStatus("Échec de la création : " + folderName);
                } catch (Exception e) {
                    System.err.println("Erreur dans Platform.runLater (error): " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
        
        // Démarrer la tâche dans un thread séparé
        Thread thread = new Thread(createTask, "create-folder-task-thread");
        thread.setDaemon(true);
        System.out.println("Démarrage du thread...");
        thread.start();
        System.out.println("Thread démarré");
    }

    /**
     * Upload un fichier de manière asynchrone
     */
    private void uploadFileAsync(File file) {
        System.out.println("\n=== uploadFileAsync() appelée ===");
        System.out.println("Fichier: " + file.getName());
        System.out.println("Taille: " + file.length());
        System.out.println("Parent ID: " + (currentFolder != null ? currentFolder.getId() : "null"));
        
        // Créer un dialogue de progression
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Upload en cours");
        progressAlert.setHeaderText("Upload de : " + file.getName());
        progressAlert.setContentText("Veuillez patienter...");
        
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressAlert.setGraphic(progressIndicator);
        
        // Retirer le bouton OK pour empêcher la fermeture
        progressAlert.getButtonTypes().clear();
        
        // Afficher le dialogue sans bloquer
        progressAlert.show();
        System.out.println("Dialog de progression affiché");
        
        // Récupérer le folder_id du dossier sélectionné
        Integer folderId = currentFolder != null ? currentFolder.getId() : null;
        
        // Créer une Task pour l'upload
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
        
        // Gestion du succès
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
            
            // Rafraîchir l'affichage
            System.out.println("Rafraîchissement de l'affichage...");
            loadData();
            showStatus("Fichier uploadé : " + file.getName());
        });
        
        // Gestion des erreurs
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
            
            showStatus("Échec de l'upload : " + file.getName());
        });
        
        // Démarrer la tâche dans un thread séparé
        Thread thread = new Thread(uploadTask, "upload-task-thread");
        thread.setDaemon(true);
        System.out.println("Démarrage du thread upload...");
        thread.start();
        System.out.println("Thread upload démarré");
    }

    /**
     * Vérifie si un dossier existe déjà
     */
    private boolean folderExists(String name, NodeItem parent) {
        return parent.getChildren().stream()
            .anyMatch(child -> child.getName().equalsIgnoreCase(name));
    }

    /**
     * Appelle l'API pour créer le dossier sur le serveur
     */
    private void createFolderOnServer(String folderName) {
        // TODO: Implémenter l'appel API
        // Exemple:
        // try {
        //     apiClient.createFolder(currentFolder.getId(), folderName);
        // } catch (Exception e) {
        //     showError("Erreur lors de la création du dossier: " + e.getMessage());
        // }
    }

    /**
     * Gère le renommage d'un dossier
     */
    private void handleRenameFolder() {
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
                // TODO: Appeler l'API pour renommer le dossier
                showStatus("Dossier renommé : " + folder.getName() + " → " + newName);
                loadData();
            }
        });
    }

    /**
     * Gère la suppression d'un dossier
     */
    private void handleDeleteFolder() {
        TreeItem<NodeItem> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;

        NodeItem folder = selected.getValue();
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le dossier");
        confirm.setHeaderText("Confirmer la suppression");
        
        // Vérifier si le dossier contient des éléments
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
            // TODO: Appeler l'API pour supprimer le dossier
            showStatus("Dossier supprimé : " + folder.getName());
            loadData();
        }
    }

    /**
     * Affiche les propriétés d'un dossier
     */
    private void handleFolderProperties() {
        TreeItem<NodeItem> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;

        NodeItem folder = selected.getValue();
        
        // Calculer les statistiques du dossier
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

    /**
     * Compte récursivement le nombre total de dossiers
     */
    private int countTotalFolders(NodeItem folder) {
        int count = folder.getChildren().size();
        for (NodeItem child : folder.getChildren()) {
            count += countTotalFolders(child);
        }
        return count;
    }

    /**
     * Compte récursivement le nombre total de fichiers
     */
    private int countTotalFiles(NodeItem folder) {
        int count = folder.getFiles().size();
        for (NodeItem child : folder.getChildren()) {
            count += countTotalFiles(child);
        }
        return count;
    }

    /**
     * Calcule récursivement la taille totale
     */
    private long calculateTotalSize(NodeItem folder) {
        long size = folder.getFiles().stream().mapToLong(FileEntry::getSize).sum();
        for (NodeItem child : folder.getChildren()) {
            size += calculateTotalSize(child);
        }
        return size;
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        double v = bytes;
        String[] units = {"Ko", "Mo", "Go", "To"};
        int i = -1;
        while (v >= 1024 && i < units.length - 1) { v /= 1024.0; i++; }
        return new DecimalFormat("0.##").format(v) + " " + units[i];
    }
}