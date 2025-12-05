package com.coffrefort.client.controllers;

import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;
import com.coffrefort.client.model.Quota;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

public class MainController {
    @FXML private TreeView<NodeItem> treeView;
    @FXML private TableView<FileEntry> table;
    @FXML private TableColumn<FileEntry, String> nameCol;
    @FXML private TableColumn<FileEntry, Long> sizeCol;
    @FXML private TableColumn<FileEntry, String> dateCol;
    @FXML private TableColumn<FileEntry, Integer> versionCol;
    @FXML private ProgressBar quotaBar;
    @FXML private Label quotaLabel;
    @FXML private Label userEmailLabel;

    private ApiClient apiClient;
    private NodeItem currentFolder;
    private String userEmail;

    private FileOperationController fileOpController;
    private VersionController versionController;
    private FolderOperationController folderOpController;
    private DragDropController dragDropController;
    private UploadController uploadController;

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
        
        fileOpController = new FileOperationController(apiClient, table, this::loadData, message -> showStatus(message));
        versionController = new VersionController(this::refreshCurrentFolder);
        folderOpController = new FolderOperationController(apiClient, treeView, this::loadData);
        uploadController = new UploadController(apiClient, table, this::loadData);
        
        setupContextMenu();
        setupTreeContextMenu();
        dragDropController = new DragDropController(table, treeView, this::refreshCurrentFolder, fileOpController);
        dragDropController.setupDragAndDrop();
        
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
                    refreshCurrentFolder();
                }
            });
        }
    }

    private void setupContextMenu() {
        if (table == null) return;

        ContextMenu contextMenu = new ContextMenu();

        MenuItem copyItem = new MenuItem("Copier");
        copyItem.setOnAction(e -> fileOpController.handleCopy());

        MenuItem cutItem = new MenuItem("Couper");
        cutItem.setOnAction(e -> fileOpController.handleCut());

        MenuItem pasteItem = new MenuItem("Coller");
        pasteItem.setOnAction(e -> fileOpController.handlePaste(currentFolder));

        SeparatorMenuItem separator1 = new SeparatorMenuItem();

        MenuItem renameItem = new MenuItem("Renommer");
        renameItem.setOnAction(e -> fileOpController.handleRename());

        MenuItem deleteItem = new MenuItem("Supprimer");
        deleteItem.setOnAction(e -> fileOpController.handleDelete());

        SeparatorMenuItem separator2 = new SeparatorMenuItem();

        MenuItem downloadItem = new MenuItem("Télécharger");
        downloadItem.setOnAction(e -> fileOpController.handleDownload());

        MenuItem shareItem = new MenuItem("Créer un lien de partage");
        shareItem.setOnAction(e -> fileOpController.handleShare());

        SeparatorMenuItem separator3 = new SeparatorMenuItem();

        MenuItem propertiesItem = new MenuItem("Propriétés");
        propertiesItem.setOnAction(e -> fileOpController.handleProperties());

        MenuItem versionsItem = new MenuItem("Historique des versions");
        versionsItem.setOnAction(e -> versionController.handleVersionHistory(table.getSelectionModel().getSelectedItem(), table));

        contextMenu.getItems().addAll(
                copyItem, cutItem, pasteItem,
                separator1,
                renameItem, deleteItem,
                separator2,
                downloadItem, shareItem,
                separator3,
                propertiesItem, versionsItem
        );

        contextMenu.setOnShowing(e -> {
            FileEntry selected = table.getSelectionModel().getSelectedItem();
            boolean hasSelection = selected != null;
            boolean hasClipboard = fileOpController.getClipboardFile() != null;

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

    private void setupTreeContextMenu() {
        if (treeView == null) return;

        treeView.setCellFactory(new Callback<TreeView<NodeItem>, TreeCell<NodeItem>>() {
            @Override
            public TreeCell<NodeItem> call(TreeView<NodeItem> param) {
                return new TreeCell<NodeItem>() {
                    private final ContextMenu cellContextMenu = createTreeContextMenu();

                    @Override
                    protected void updateItem(NodeItem item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setContextMenu(null);
                        } else {
                            setText(item.getName());
                            setContextMenu(cellContextMenu);
                        }
                    }
                };
            }
        });
    }

    private ContextMenu createTreeContextMenu() {
        ContextMenu treeContextMenu = new ContextMenu();

        MenuItem newFolderItem = new MenuItem("Nouveau dossier");
        newFolderItem.setOnAction(e -> folderOpController.handleCreateFolder(currentFolder));

        MenuItem renameFolderItem = new MenuItem("Renommer");
        renameFolderItem.setOnAction(e -> folderOpController.handleRenameFolder());

        MenuItem deleteFolderItem = new MenuItem("Supprimer");
        deleteFolderItem.setOnAction(e -> folderOpController.handleDeleteFolder());

        SeparatorMenuItem separator = new SeparatorMenuItem();

        MenuItem folderPropsItem = new MenuItem("Propriétés");
        folderPropsItem.setOnAction(e -> folderOpController.handleFolderProperties());

        MenuItem refreshItem = new MenuItem("Actualiserrr");
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

        treeContextMenu.setOnShowing(e -> {
            TreeItem<NodeItem> selected = treeView.getSelectionModel().getSelectedItem();
            boolean hasSelection = selected != null && selected.getValue() != null;

            renameFolderItem.setDisable(!hasSelection);
            deleteFolderItem.setDisable(!hasSelection);
            folderPropsItem.setDisable(!hasSelection);
        });

        return treeContextMenu;
    }

    @FXML
    private void handleUpload() {
        if (currentFolder == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Sélection requise");
            alert.setHeaderText("Aucun dossier sélectionné");
            alert.setContentText("Veuillez sélectionner un dossier avant d'uploader un fichier.");
            alert.showAndWait();
            return;
        }
        if (currentFolder.getId() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Dossier invalide");
            alert.setContentText("L'ID du dossier sélectionné est invalide. Veuillez sélectionner un autre dossier.");
            alert.showAndWait();
            return;
        }
        uploadController.handleUpload(currentFolder);
    }

    @FXML
    private void handleCreateFolder() {
        folderOpController.handleCreateFolder(currentFolder);
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText("Confirmer la déconnexion");
        confirm.setContentText("Voulez-vous vraiment vous déconnecter ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                apiClient.clearToken();
                System.out.println("Token nettoyé, déconnexion effectuée");
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

    private void returnToLoginScreen() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/coffrefort/client/login.fxml")
            );
            
            loader.setControllerFactory(type -> {
                if (type == LoginController.class) {
                    LoginController controller = new LoginController();
                    controller.setApiClient(apiClient);
                    controller.setOnSuccess((email) -> {
                        userEmail = email;
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
            
            javafx.stage.Stage loginStage = new javafx.stage.Stage();
            loginStage.setTitle("Coffre-fort numérique — Connexion");
            loginStage.setScene(new javafx.scene.Scene(root, 420, 320));
            
            javafx.stage.Stage currentStage = (javafx.stage.Stage) userEmailLabel.getScene().getWindow();
            currentStage.close();
            
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

    private void showStatus(String message) {
        if (quotaLabel != null) {
            String currentText = quotaLabel.getText();
            quotaLabel.setText(message);
            
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

    private void refreshCurrentFolder() {
        if (currentFolder != null) {
            table.getItems().setAll(currentFolder.getFiles());
        }
    }

    private String humanSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#")
            .format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
